package dev.aethereal.agui.surface

import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.text.MenuText
import dev.aethereal.agui.text.TextContext
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class DynamicIcon internal constructor(private val item: ItemStack, private val context: TextContext) {
    private val meta = checkNotNull(item.itemMeta) { "Предмет не поддерживает ItemMeta" }
    fun name(value: String) {
        MenuEngine.requireSync()
        meta.customName(context.render(MenuText(value)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
    }
    fun lore(vararg lines: String) {
        MenuEngine.requireSync()
        meta.lore(lines.map { context.render(MenuText(it)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
    }
    fun glow(value: Boolean = true) { MenuEngine.requireSync(); meta.setEnchantmentGlintOverride(value) }
    fun enchant(enchantment: Enchantment, level: Int) {
        MenuEngine.requireSync()
        require(level > 0) { "Уровень зачарования должен быть положительным" }
        meta.addEnchant(enchantment, level, true)
    }
    fun removeEnchant(enchantment: Enchantment) { MenuEngine.requireSync(); meta.removeEnchant(enchantment) }
    internal fun apply(): ItemStack { check(item.setItemMeta(meta)) { "Не удалось применить мету предмета" }; return item }
}
