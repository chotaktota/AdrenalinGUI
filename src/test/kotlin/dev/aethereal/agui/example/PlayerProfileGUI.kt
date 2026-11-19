package dev.aethereal.agui.example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class PlayerProfileGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ....P....
            .........
        """.trimIndent())) {
            title("<gold>Профиль %player_name%")
            iconAt(pos('P')) {
                type(Material.PAPER)
                name("<white>%player_name%")
                loreLine("<gray>Баланс: <yellow>%vault_eco_balance%")
                loreLine("<green>Нажмите, чтобы закрыть")
                allFlags()
                onClick { close() }
            }
        }
    }
}
