package dev.aethereal.agui.text

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player

internal object PapiParser {
    fun parse(player: Player, text: String): String = PlaceholderAPI.setPlaceholders(player, text)
}
