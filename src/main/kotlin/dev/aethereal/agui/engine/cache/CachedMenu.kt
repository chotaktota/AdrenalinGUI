package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.text.TextContext
import dev.aethereal.agui.surface.item.Icon
import dev.aethereal.agui.surface.layout.Pattern
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.IdentityHashMap

internal class CachedMenu(pattern: Pattern) {
    val size = pattern.size
    val inputSlots = pattern.inputs.toSet()
    val linkedInputs = pattern.linkedInputs.map { (source, value) -> CachedInput(source, value.first, value.second) }
    val conditionalSlots = pattern.icons.filterValues { it.conditions.isNotEmpty() }.keys.toIntArray()
    val inputSources = pattern.linkedInputs.keys.toSet()
    val inputEvents = pattern.inputEvents.mapValues { CachedInputEvents(it.value) } +
        pattern.linkedInputs.values.associate { it.first to CachedInputEvents(it.second) }
    private val caption = pattern.caption
    private val nodes = arrayOfNulls<CachedNode>(size)
    val animation: CachedAnimation?

    fun animatedContents() = dev.aethereal.agui.engine.session.AnimatedContents(nodes.copyOf())

    init {
        require(inputSources.none { it in inputSlots || it in pattern.icons }) { "Источник поля пересекается с другим полем или иконкой" }
        require(inputSlots.none { it in pattern.icons }) { "Поле для предметов пересекается с иконкой" }
        require(pattern.animation?.steps?.values?.flatten()?.none { op -> op.slots.any { it in inputSlots || it in inputSources } } != false) {
            "Анимация пересекается с полем для предметов"
        }
        val built = IdentityHashMap<Icon, CachedNode>()
        fun compile(icon: Icon): CachedNode = built.getOrPut(icon) {
            CachedNode(
                icon.compile(), icon.handler, icon.visibility, icon.cooldownTicks,
                icon.conditions.toList(), icon.disabledStyle, icon.denied
            )
        }
        pattern.icons.forEach { (slot, icon) -> nodes[slot] = compile(icon) }
        animation = pattern.animation?.let { CachedAnimation(it, ::compile) }
    }

    fun title(context: TextContext) = context.render(caption)

    fun populate(inventory: Inventory, context: TextContext) {
        val rendered = IdentityHashMap<CachedNode, ItemStack?>()
        inventory.contents = Array(size) { slot ->
            nodes[slot]?.render(context, rendered, includeDisabled = false)?.clone()
        }
    }

    fun refresh(inventory: Inventory, context: TextContext, slots: IntArray) {
        require(slots.all { it in 0 until size }) { "Слот за пределами меню" }
        val rendered = IdentityHashMap<CachedNode, ItemStack?>()
        for (slot in slots.distinct()) {
            val next = nodes[slot]?.render(context, rendered)?.clone()
            if (inventory.getItem(slot) != next) inventory.setItem(slot, next)
        }
    }

    fun nodeAt(slot: Int): CachedNode? = nodes.getOrNull(slot)
}
