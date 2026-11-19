package dev.aethereal.agui.surface

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface GUISurface {
    val plugin: Plugin
    fun open(player: Player)
    fun invalidateCache()
}
