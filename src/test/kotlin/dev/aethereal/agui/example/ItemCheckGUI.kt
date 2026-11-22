package dev.aethereal.agui.example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.plugin.java.JavaPlugin

class ItemCheckGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ..X.I.B..
            .........
        """.trimIndent())) {
            title("<red>Проверка предмета")
            val source = pos('X').first()
            val input = pos('I').first()
            val button = pos('B').first()

            inputAt(source, input) {
                placeholder {
                    type(Material.BARRIER)
                    name("<red>Положите алмазный меч")
                    loreLine("<gray>ЛКМ — стак, ПКМ — один")
                }
                acceptIf { item -> item.type == Material.DIAMOND_SWORD }
                loreLine("<gray>Предмет в поле проверки")
                onInsert { sound(Sound.BLOCK_ANVIL_PLACE, volume = 0.5f) }
                onTake { sound(Sound.ENTITY_ITEM_PICKUP) }
                onChange { refresh(button) }
            }

            iconAt(button) {
                type(Material.ANVIL)
                name("<green>Проверить предмет")
                enabledIf("<red>Сначала положите алмазный меч") { viewer ->
                    viewer.openInventory.topInventory.getItem(input)?.type == Material.DIAMOND_SWORD
                }
                disabled { reason ->
                    name("<gray>Проверить предмет")
                    lore(reason)
                }
                onClick {
                    val item = inputItem(input) ?: return@onClick
                    player.sendMessage("В поле находится " + item.type.name)
                    sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
                }
            }
        }
    }
}
