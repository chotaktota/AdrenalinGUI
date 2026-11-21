package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.text.TextContext
import dev.aethereal.agui.surface.item.Icon
import dev.aethereal.agui.surface.layout.Pattern
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.IdentityHashMap

internal class CachedMenu(pattern: Pattern) {
    val size = pattern.size
    private val caption = pattern.caption
    private val nodes = arrayOfNulls<CachedNode>(size)
    val animation: CachedAnimation?

    fun animatedContents() = dev.aethereal.agui.engine.session.AnimatedContents(nodes.copyOf())

    init {
        val built = IdentityHashMap<Icon, CachedNode>()
        fun compile(icon: Icon): CachedNode = built.getOrPut(icon) {
            CachedNode(icon.compile(), icon.handler, icon.visibility)
        }
        pattern.icons.forEach { (slot, icon) -> nodes[slot] = compile(icon) }
        animation = pattern.animation?.let { CachedAnimation(it, ::compile) }
    }

    fun title(context: TextContext) = context.render(caption)

    fun populate(inventory: Inventory, context: TextContext) {
        val rendered = IdentityHashMap<CachedNode, ItemStack?>()
        inventory.contents = Array(size) { slot ->
            nodes[slot]?.render(context, rendered)?.clone()
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
