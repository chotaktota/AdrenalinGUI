package dev.aethereal.agui.menu.cache

import dev.aethereal.agui.menu.text.MenuText
import dev.aethereal.agui.menu.text.TextContext
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack

internal class CachedItem(
    private val prototype: ItemStack,
    private val name: MenuText?,
    private val lore: List<MenuText>
) {
    private val dynamicName = name?.dynamic == true
    private val dynamicLore = lore.any { it.dynamic }

    fun render(context: TextContext): ItemStack {
        if (!dynamicName && !dynamicLore) return prototype
        val item = prototype.clone()
        val meta = checkNotNull(item.itemMeta) { "Предмет не поддерживает ItemMeta" }
        if (dynamicName) meta.customName(context.render(checkNotNull(name))
            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
        if (dynamicLore) meta.lore(lore.map {
            context.render(it).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        })
        check(item.setItemMeta(meta)) { "Не удалось применить мету предмета" }
        return item
    }
}
