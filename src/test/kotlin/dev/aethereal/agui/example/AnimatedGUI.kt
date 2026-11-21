package dev.aethereal.agui.example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class AnimatedGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern(".........\n....X....\n.........")) {
            title("<red>Анимация")
            iconAt(pos('X')) {
                type(Material.PAPER)
                name("<white>Нажмите")
                onClick { close() }
            }
            animation {
                at(0) { hide(pos('X')) }
                at(10) { show(pos('X')) }
                at(20) {
                    change(pos('X')) { player ->
                        name("<gold>${player.name}")
                        lore("&7Баланс: %vault_eco_balance%")
                        glow()
                    }
                }
                at(30) {
                    set(pos('X')) {
                        type(Material.EMERALD)
                        name("<green>Готово")
                        onClick { close() }
                    }
                }
                loop(60)
            }
        }
    }
}
