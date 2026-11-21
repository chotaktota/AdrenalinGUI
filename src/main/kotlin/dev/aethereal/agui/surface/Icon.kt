package dev.aethereal.agui.surface

import dev.aethereal.agui.menu.MenuEngine

class Icon : ItemBuilder() {
    internal var update: (DynamicIcon.(org.bukkit.entity.Player) -> Unit)? = null
    fun dynamic(action: DynamicIcon.(org.bukkit.entity.Player) -> Unit) { MenuEngine.requireSync(); update = action }
    internal var handler: (GUIClick.() -> Unit)? = null
    fun onClick(action: GUIClick.() -> Unit) { MenuEngine.requireSync(); handler = action }
}
