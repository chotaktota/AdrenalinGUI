package dev.aethereal.agui.surface

import net.kyori.adventure.text.format.TextDecoration
import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.menu.cache.CachedItem
import dev.aethereal.agui.text.MenuText
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

open class ItemBuilder {
    private var material = Material.STONE
    private var count = 1
    private var displayName: MenuText? = null
    private val lines = mutableListOf<MenuText>()
    private var glowing = false
    private var hideFlags = false
    fun type(value: Material) { MenuEngine.requireSync(); material = value }
    fun amount(value: Int) { MenuEngine.requireSync(); count = value }
    fun name(value: String) {
        MenuEngine.requireSync()
        displayName = MenuText(value)
    }
    fun loreLine(value: String) {
        MenuEngine.requireSync()
        lines.add(MenuText(value))
    }
    fun glow(value: Boolean = true) { MenuEngine.requireSync(); glowing = value }
    fun allFlags() { MenuEngine.requireSync(); hideFlags = true }
    internal fun compile(): CachedItem = CachedItem(build(), displayName, lines.toList(), (this as? Icon)?.update)

    private fun build(): ItemStack {
        MenuEngine.requireSync()
        require(material.isItem && !material.isAir) { "Материал $material не является непустым предметом" }
        require(count in 1..99) { "Количество предметов должно быть от 1 до 99" }
        val item = ItemStack.of(material, count)
        require(count <= item.maxStackSize) { "Количество превышает размер стака предмета: ${item.maxStackSize}" }
        val meta = checkNotNull(item.itemMeta) { "Предмет не поддерживает ItemMeta" }
        meta.customName(displayName?.component?.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
        meta.lore(lines.map { it.component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
        meta.setEnchantmentGlintOverride(glowing)
        if (hideFlags) meta.addItemFlags(*ItemFlag.entries.toTypedArray())
        check(item.setItemMeta(meta)) { "Не удалось применить мету предмета" }
        return item
    }

}
