package dev.aethereal.agui.listener

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.engine.session.MenuSession
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent

internal class ClickListener(private val engine: MenuEngine) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onClick(event: InventoryClickEvent) {
        val session = event.view.topInventory.holder as? MenuSession ?: return
        val wasCancelled = event.isCancelled
        event.isCancelled = true
        if (!wasCancelled && event.whoClicked.uniqueId == session.player.uniqueId &&
            event.rawSlot in 0 until event.view.topInventory.size) {
            session.dispatch(event.rawSlot, event.click)
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is MenuSession) event.isCancelled = true
    }
    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        (event.inventory.holder as? MenuSession)?.dispose()
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        if (engine.chatInputs.capture(event.player, event.originalMessage())) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onOpen(event: InventoryOpenEvent) {
        (event.player as? Player)?.let { engine.chatInputs.clear(it) }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        engine.chatInputs.clear(event.player)
        (event.player.openInventory.topInventory.holder as? MenuSession)?.dispose()
    }
    @EventHandler
    fun onDisable(event: PluginDisableEvent) {
        engine.close(event.plugin)
    }
}
