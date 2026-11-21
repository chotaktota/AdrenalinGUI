package dev.aethereal.agui.surface

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
    fun refresh(vararg slots: Int) {
        MenuEngine.requireSync()
        val session = player.openInventory.topInventory.holder as? dev.aethereal.agui.menu.session.MenuSession ?: return
        session.refresh(if (slots.isEmpty()) IntArray(session.inventory.size) { it } else slots)
    }
    fun close() { MenuEngine.requireSync(); player.closeInventory() }
    fun openPage(page: String) {
        MenuEngine.requireSync()
        checkNotNull(navigate) { "Переход между страницами доступен только в MultiGUISurface" }.invoke(page)
    }
}
