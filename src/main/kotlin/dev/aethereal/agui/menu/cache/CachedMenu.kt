package dev.aethereal.agui.menu.cache

import dev.aethereal.agui.surface.icon.Icon
import dev.aethereal.agui.surface.icon.action.GUIClick
import dev.aethereal.agui.surface.pattern.Pattern
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.IdentityHashMap
internal class CachedMenu(pattern: Pattern) {
    val size = pattern.size
    val caption = pattern.caption
    private val items = arrayOfNulls<ItemStack>(size)
    private val handlers = arrayOfNulls<GUIClick.() -> Unit>(size)

    init {
        val built = IdentityHashMap<Icon, ItemStack>()
        pattern.icons.forEach { (slot, icon) ->
            items[slot] = built.getOrPut(icon) { icon.build() }
            handlers[slot] = icon.handler
        }
    }

    fun populate(inventory: Inventory) {
        inventory.contents = Array(size) { items[it]?.clone() }
    }

    fun handler(slot: Int): (GUIClick.() -> Unit)? = handlers.getOrNull(slot)
}
