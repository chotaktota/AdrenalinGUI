package dev.aethereal.benchmark

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.Base64

internal fun verifyItemProperties(plugin: Plugin, viewer: Player) {
    val engine = (plugin.server.pluginManager.getPlugin("AdrenalinGUI") as AdrenalinGUI).engine
    val key = NamespacedKey(plugin, "verification")
    val color = Color.fromRGB(0xE64049)
    val hash = "a".repeat(64)
    val url = "https://textures.minecraft.net/texture/$hash"
    val encoded = Base64.getEncoder().encodeToString("""{"textures":{"SKIN":{"url":"$url"}}}""".encodeToByteArray())
    var rank = 1
    val menu = object : SingleGUISurface(1, plugin) {
        override fun onUpdateNodes() {
            with(newPattern(".........")) {
                iconAt(0) {
                    type(Material.DIAMOND_SWORD)
                    enchant(Enchantment.SHARPNESS, 5)
                    attribute(Attribute.ATTACK_DAMAGE, key, 2.0, slot = EquipmentSlotGroup.MAINHAND)
                    unbreakable()
                    damage(10)
                    nbt(key, PersistentDataType.STRING, "меч")
                    modelData { floats = listOf(12f); strings = listOf("меч") }
                    itemModel(key)
                }
                iconAt(1) { type(Material.POTION); potionColor(color) }
                listOf(hash, url, encoded).forEachIndexed { index, texture ->
                    iconAt(index + 2) {
                        type(Material.PLAYER_HEAD)
                        headTexture(texture)
                        equippable(EquipmentSlot.HEAD) { model = key }
                    }
                }
                iconAt(5) { type(Material.ENCHANTED_BOOK); enchant(Enchantment.UNBREAKING, 2) }
                iconAt(6) {
                    type(Material.PAPER)
                    dynamic {
                        nbt(key, PersistentDataType.INTEGER, rank)
                        modelData(rank)
                    }
                }
            }
        }
    }
    menu.open(viewer)
    try {
        val inventory = viewer.openInventory.topInventory
        fun meta(slot: Int) = checkNotNull(inventory.getItem(slot)?.itemMeta)
        val sword = meta(0)
        check(sword.getEnchantLevel(Enchantment.SHARPNESS) == 5)
        check(sword.getAttributeModifiers(Attribute.ATTACK_DAMAGE)?.single()?.key == key)
        check(sword.isUnbreakable && (sword as org.bukkit.inventory.meta.Damageable).damage == 10)
        check(sword.persistentDataContainer.get(key, PersistentDataType.STRING) == "меч")
        check(sword.customModelDataComponent.floats == listOf(12f) &&
            sword.customModelDataComponent.strings == listOf("меч") && sword.itemModel == key)
        check((meta(1) as PotionMeta).color == color)
        for (slot in 2..4) {
            val head = meta(slot) as SkullMeta
            check(head.playerProfile?.textures?.skin.toString() == url)
            check(head.equippable.slot == EquipmentSlot.HEAD && head.equippable.model == key)
        }
        check((meta(5) as EnchantmentStorageMeta).getStoredEnchantLevel(Enchantment.UNBREAKING) == 2)
        rank = 2
        engine.refresh(viewer, 6)
        check(meta(6).persistentDataContainer.get(key, PersistentDataType.INTEGER) == 2)
        check(meta(6).customModelDataComponent.floats == listOf(2f))
    } finally {
        viewer.closeInventory()
    }
}
