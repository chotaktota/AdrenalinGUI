package dev.aethereal.agui.example

import dev.aethereal.agui.AdrenalinGUI
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class HelpExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        val engine = JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine
        engine.menus.register("help", HelpGUI(this))
        requireNotNull(getCommand("helpui")).setExecutor { sender, _, _, _ ->
            if (sender is Player) engine.open("help", sender)
            true
        }
    }
}
