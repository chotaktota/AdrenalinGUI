package dev.aethereal.agui.text

import dev.aethereal.agui.engine.MenuEngine
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

internal class TextRenderer(private val plugin: Plugin) {
    fun forPlayer(player: Player): TextContext {
        MenuEngine.requireSync()
        return if (plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            TextContext(player) { PapiParser.parse(player, it) }
        } else TextContext(player) { it }
    }
}
