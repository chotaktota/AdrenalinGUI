package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.text.TextContext
import dev.aethereal.agui.surface.item.Icon
import dev.aethereal.agui.surface.item.GUIClick
import dev.aethereal.agui.surface.layout.Pattern
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.IdentityHashMap

internal class CachedMenu(pattern: Pattern) {
    val size = pattern.size
    private val caption = pattern.caption
    private val items = arrayOfNulls<CachedItem>(size)
    private val handlers = arrayOfNulls<GUIClick.() -> Unit>(size)

    private val nodes = if (pattern.animation == null) null else arrayOfNulls<CachedNode>(size)
    val animation: CachedAnimation?
    fun animatedContents() = dev.aethereal.agui.engine.session.AnimatedContents(checkNotNull(nodes).copyOf())
    init {
        val built = IdentityHashMap<Icon, CachedItem>()
        fun compile(icon: Icon): CachedNode = CachedNode(built.getOrPut(icon) { icon.compile() }, icon.handler)
        pattern.icons.forEach { (slot, icon) ->
            items[slot] = built.getOrPut(icon) { icon.compile() }
            nodes?.set(slot, CachedNode(checkNotNull(items[slot]), icon.handler))
            handlers[slot] = icon.handler
        }
        animation = pattern.animation?.let { CachedAnimation(it, ::compile) }
    }

    fun title(context: TextContext) = context.render(caption)

    fun populate(inventory: Inventory, context: TextContext) {
        val rendered = IdentityHashMap<CachedItem, ItemStack>()
        inventory.contents = Array(size) { slot ->
            items[slot]?.let { rendered.getOrPut(it) { it.render(context) }.clone() }
        }
    }

    fun refresh(inventory: Inventory, context: TextContext, slots: IntArray) {
        require(slots.all { it in 0 until size }) { "Слот за пределами меню" }
        val rendered = IdentityHashMap<CachedItem, ItemStack>()
        for (slot in slots.distinct()) {
            val next = items[slot]?.let { rendered.getOrPut(it) { it.render(context) }.clone() }
            if (inventory.getItem(slot) != next) inventory.setItem(slot, next)
        }
    }

    fun handler(slot: Int): (GUIClick.() -> Unit)? = handlers.getOrNull(slot)
}
