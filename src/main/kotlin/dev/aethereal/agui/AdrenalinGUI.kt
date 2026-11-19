package dev.aethereal.agui

import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.surface.icon.action.listener.ClickListener
import org.bukkit.plugin.java.JavaPlugin

class AdrenalinGUI : JavaPlugin() {
    lateinit var engine: MenuEngine
        private set

    override fun onEnable() {
        engine = MenuEngine(this)
        server.pluginManager.registerEvents(ClickListener(engine), this)
    }

    override fun onDisable() {
        if (::engine.isInitialized) engine.close()
    }
}
