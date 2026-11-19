package dev.aethereal.agui.surface.icon.item

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.format.TextDecoration
import dev.aethereal.agui.menu.MenuEngine
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

open class ItemBuilder {
    private var material = Material.STONE
    private var count = 1
    private var displayName: Component? = null
    private val lines = mutableListOf<Component>()
    private var glowing = false
    private var hideFlags = false
    fun type(value: Material) { MenuEngine.requireSync(); material = value }
    fun amount(value: Int) { MenuEngine.requireSync(); count = value }
    fun name(value: String) {
        MenuEngine.requireSync()
        displayName = miniMessage.deserialize(value)
    }
    fun loreLine(value: String) {
        MenuEngine.requireSync()
        lines.add(miniMessage.deserialize(value))
    }
    fun glow(value: Boolean = true) { MenuEngine.requireSync(); glowing = value }
    fun allFlags() { MenuEngine.requireSync(); hideFlags = true }
    internal fun build(): ItemStack {
        MenuEngine.requireSync()
        require(material.isItem && !material.isAir) { "Материал $material не является непустым предметом" }
        require(count in 1..99) { "Количество предметов должно быть от 1 до 99" }
        val item = ItemStack.of(material, count)
        require(count <= item.maxStackSize) { "Количество превышает размер стака предмета: ${item.maxStackSize}" }
        val meta = checkNotNull(item.itemMeta) { "Предмет не поддерживает ItemMeta" }
        meta.customName(displayName?.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
        meta.lore(lines.map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
        meta.setEnchantmentGlintOverride(glowing)
        if (hideFlags) meta.addItemFlags(*ItemFlag.entries.toTypedArray())
        check(item.setItemMeta(meta)) { "Не удалось применить мету предмета" }
        return item
    }

    private companion object {
        val miniMessage = MiniMessage.miniMessage()
    }
}
