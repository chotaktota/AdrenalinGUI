package dev.aethereal.agui.surface.item

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.text.MenuText
import org.bukkit.Material

enum class InputTransfer { CLICK, ALL, ONE }

class InputField internal constructor() {
    internal var transferMode = InputTransfer.CLICK
    internal val lines = mutableListOf<MenuText>()
    internal val icon = Icon().apply { type(Material.BARRIER) }

    fun placeholder(configure: Icon.() -> Unit) {
        MenuEngine.requireSync()
        icon.configure()
    }

    fun loreLine(value: String) {
        MenuEngine.requireSync()
        lines.add(MenuText(value))
    }

    fun transfer(mode: InputTransfer) {
        MenuEngine.requireSync()
        transferMode = mode
    }
}
