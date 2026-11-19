package dev.aethereal.agui.surface.icon.action

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import dev.aethereal.agui.menu.MenuEngine

class GUIClick internal constructor(
    val player: Player,
    val slot: Int,
    val click: ClickType,
    val page: String? = null,
    private val navigate: ((String) -> Unit)? = null
) {
    val isLeftClick: Boolean get() = click.isLeftClick
    val isRightClick: Boolean get() = click.isRightClick
    val isShiftClick: Boolean get() = click.isShiftClick
    fun close() { player.closeInventory() }
    fun openPage(page: String) {
        MenuEngine.requireSync()
        checkNotNull(navigate) { "Переход между страницами доступен только в MultiGUISurface" }.invoke(page)
    }
}
