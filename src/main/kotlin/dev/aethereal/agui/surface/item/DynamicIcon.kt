package dev.aethereal.agui.surface.item

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.text.MenuText
import dev.aethereal.agui.text.TextContext
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class DynamicIcon internal constructor(private val item: ItemStack, private val context: TextContext) : ItemProperties() {
    private val meta = checkNotNull(item.itemMeta) { "Предмет не поддерживает ItemMeta" }
    override fun editMeta(action: ItemMeta.() -> Unit) { meta.action() }

    fun name(value: String) {
        MenuEngine.requireSync()
        meta.customName(context.render(MenuText(value)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
    }
    fun lore(vararg lines: String) {
        MenuEngine.requireSync()
        meta.lore(lines.map { context.render(MenuText(it)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
    }
    fun glow(value: Boolean = true) { MenuEngine.requireSync(); meta.setEnchantmentGlintOverride(value) }
    fun enchant(enchantment: Enchantment, level: Int) = enchant(enchantment, level, unsafe = true)
    fun removeEnchant(enchantment: Enchantment) = meta {
        if (this is org.bukkit.inventory.meta.EnchantmentStorageMeta) removeStoredEnchant(enchantment)
        else removeEnchant(enchantment)
    }
    internal fun apply(): ItemStack { check(item.setItemMeta(meta)) { "Не удалось применить мету предмета" }; return item }
}
