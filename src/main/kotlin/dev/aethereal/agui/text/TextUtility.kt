package dev.aethereal.agui.text

import dev.aethereal.agui.engine.MenuEngine
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

internal class TextUtility(private val plugin: Plugin) {
    fun forPlayer(player: Player): TextContext {
        MenuEngine.requireSync()
        return if (plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            TextContext(player) { PapiParser.parse(player, it) }
        } else TextContext(player) { it }
    }
}

internal class TextContext(val player: Player? = null, private val resolve: (String) -> String) {
    private val rendered = hashMapOf<String, Component>()
    private val values = hashMapOf<String, String>()

    fun render(text: MenuText): Component =
        if (!text.dynamic) text.component
        else rendered.getOrPut(text.source) {
            text.render { text.substitute { token -> values.getOrPut(token) { resolve(token) } } }
        }
}

private object PapiParser {
    fun parse(player: Player, text: String): String = PlaceholderAPI.setPlaceholders(player, text)
}
