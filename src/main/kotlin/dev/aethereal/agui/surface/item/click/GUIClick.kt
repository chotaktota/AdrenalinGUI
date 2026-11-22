package dev.aethereal.agui.surface.item.click

import dev.aethereal.agui.engine.session.MenuSession
import dev.aethereal.agui.surface.item.click.action.OtherAction
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

class GUIClick internal constructor(
    player: Player,
    val slot: Int,
    val click: ClickType,
    val page: String? = null,
    navigate: ((String) -> Unit)?,
    session: MenuSession
) : OtherAction(player, navigate, session) {
    val isLeftClick: Boolean get() = click.isLeftClick
    val isRightClick: Boolean get() = click.isRightClick
    val isShiftClick: Boolean get() = click.isShiftClick
}
