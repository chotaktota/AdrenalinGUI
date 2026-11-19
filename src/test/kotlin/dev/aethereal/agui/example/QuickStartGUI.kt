package dev.aethereal.agui.example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class QuickStartGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ....X....
            .........
        """.trimIndent())) {
            title("<gradient:#55FFAA:#55AAFF>Welcome</gradient>")
            iconAt(pos('X')) {
                type(Material.ENDER_EYE)
                name("<white>Explore the server")
                loreLine("<gray>Your menu. Your Kotlin code.")
                loreLine("<green>Click to close")
                glow()
                allFlags()
                onClick { close() }
            }
        }
    }
}
