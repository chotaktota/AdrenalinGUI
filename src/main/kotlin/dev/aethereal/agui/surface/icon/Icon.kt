package dev.aethereal.agui.surface.icon

import dev.aethereal.agui.surface.icon.action.GUIClick
import dev.aethereal.agui.surface.icon.item.ItemBuilder
import dev.aethereal.agui.menu.MenuEngine

class Icon : ItemBuilder() {
    internal var handler: (GUIClick.() -> Unit)? = null
    fun onClick(action: GUIClick.() -> Unit) { MenuEngine.requireSync(); handler = action }
}
