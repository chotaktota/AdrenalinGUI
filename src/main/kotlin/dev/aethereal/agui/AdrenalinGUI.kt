package dev.aethereal.agui

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.listener.ClickListener
import org.bukkit.plugin.java.JavaPlugin

class AdrenalinGUI : JavaPlugin() {
    lateinit var engine: MenuEngine
        private set

    override fun onEnable() {
        engine = MenuEngine(this)
        server.pluginManager.registerEvents(ClickListener(engine), this)
        val command = checkNotNull(getCommand("guis")) { "Команда guis отсутствует в plugin.yml" }
        val executor = dev.aethereal.agui.command.GuisCommand(engine)
        command.setExecutor(executor)
        command.tabCompleter = executor
    }

    override fun onDisable() {
        if (::engine.isInitialized) engine.close()
    }
}
