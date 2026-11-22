package dev.aethereal.agui.example

import dev.aethereal.agui.AdrenalinGUI
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class DocumentationPlugin : JavaPlugin() {
    override fun onEnable() {
        val engine = JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine
        engine.menus.register("main", QuickStartGUI(this))
        requireNotNull(getCommand("menu")).setExecutor { sender, _, _, _ ->
            if (sender is Player) engine.open("main", sender)
            true
        }
    }
}
