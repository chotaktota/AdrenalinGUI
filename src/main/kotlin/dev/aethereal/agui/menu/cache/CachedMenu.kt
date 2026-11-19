package dev.aethereal.agui.menu.cache

import dev.aethereal.agui.menu.text.TextContext
import dev.aethereal.agui.surface.icon.Icon
import dev.aethereal.agui.surface.icon.action.GUIClick
import dev.aethereal.agui.surface.pattern.Pattern
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.IdentityHashMap

internal class CachedMenu(pattern: Pattern) {
    val size = pattern.size
    private val caption = pattern.caption
    private val items = arrayOfNulls<CachedItem>(size)
    private val handlers = arrayOfNulls<GUIClick.() -> Unit>(size)

    init {
        val built = IdentityHashMap<Icon, CachedItem>()
        pattern.icons.forEach { (slot, icon) ->
            items[slot] = built.getOrPut(icon) { icon.compile() }
            handlers[slot] = icon.handler
        }
    }

    fun title(context: TextContext) = context.render(caption)

    fun populate(inventory: Inventory, context: TextContext) {
        val rendered = IdentityHashMap<CachedItem, ItemStack>()
        inventory.contents = Array(size) { slot ->
            items[slot]?.let { rendered.getOrPut(it) { it.render(context) }.clone() }
        }
    }

    fun handler(slot: Int): (GUIClick.() -> Unit)? = handlers.getOrNull(slot)
}
