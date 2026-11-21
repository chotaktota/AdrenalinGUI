package dev.aethereal.agui.menu.cache

import dev.aethereal.agui.text.MenuText
import dev.aethereal.agui.text.TextContext
import dev.aethereal.agui.surface.icon.DynamicIcon
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal class CachedItem(
    private val prototype: ItemStack,
    private val name: MenuText?,
    private val lore: List<MenuText>,
    private val update: (DynamicIcon.(Player) -> Unit)? = null
) {
    private val dynamicName = name?.dynamic == true
    private val dynamicLore = lore.any { it.dynamic }
    private val rendered = object : LinkedHashMap<RenderKey, ItemStack>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<RenderKey, ItemStack>?) = size > 16
    }

    fun render(context: TextContext): ItemStack {
        val base = if (!dynamicName && !dynamicLore) prototype else {
            val components = ArrayList<Component?>(lore.size + 1)
            components.add(name?.let { context.render(it) })
            lore.forEach { components.add(context.render(it)) }
            rendered.getOrPut(RenderKey(components)) {
                val item = prototype.clone()
                val meta = checkNotNull(item.itemMeta) { "Предмет не поддерживает ItemMeta" }
                if (dynamicName) meta.customName(components[0]?.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                if (dynamicLore) meta.lore(components.drop(1).map {
                    checkNotNull(it).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                })
                check(item.setItemMeta(meta)) { "Не удалось применить мету предмета" }
                item
            }
        }
        if (update == null) return base
        val item = base.clone()
        val dynamic = DynamicIcon(item, context)
        update.invoke(dynamic, checkNotNull(context.player))
        return dynamic.apply()
    }
    private class RenderKey(private val components: List<Component?>) {
        private val hash = components.fold(1) { result, component -> 31 * result + System.identityHashCode(component) }
        override fun hashCode() = hash
        override fun equals(other: Any?): Boolean = other is RenderKey &&
            components.size == other.components.size && components.indices.all { components[it] === other.components[it] }
    }
}
