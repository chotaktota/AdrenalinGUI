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
    private val allowedActions = setOf(
        org.bukkit.event.inventory.InventoryAction.PICKUP_ALL,
        org.bukkit.event.inventory.InventoryAction.PICKUP_SOME,
        org.bukkit.event.inventory.InventoryAction.PICKUP_HALF,
        org.bukkit.event.inventory.InventoryAction.PICKUP_ONE,
        org.bukkit.event.inventory.InventoryAction.PLACE_ALL,
        org.bukkit.event.inventory.InventoryAction.PLACE_SOME,
        org.bukkit.event.inventory.InventoryAction.PLACE_ONE,
        org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR,
        org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP,
        org.bukkit.event.inventory.InventoryAction.DROP_ALL_SLOT,
        org.bukkit.event.inventory.InventoryAction.DROP_ONE_SLOT,
        org.bukkit.event.inventory.InventoryAction.DROP_ALL_CURSOR,
        org.bukkit.event.inventory.InventoryAction.DROP_ONE_CURSOR,
        org.bukkit.event.inventory.InventoryAction.NOTHING
    )

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onClick(event: InventoryClickEvent) {
        val session = event.view.topInventory.holder as? MenuSession ?: return
        if (event.whoClicked.uniqueId != session.player.uniqueId || !session.isActive) {
            event.isCancelled = true
            return
        }
        if (event.isCancelled) return
        session.flushInputChanges()
        if (session.isLinked(event.rawSlot)) {
            event.isCancelled = true
            session.linkedClick(event.rawSlot, event.click)
            return
        }
        val top = event.rawSlot in 0 until event.view.topInventory.size
        if (event.action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY &&
            (!top || session.isInput(event.rawSlot))) {
            event.isCancelled = true
            session.shiftInput(event.rawSlot)
            return
        }
        if (top && session.isInput(event.rawSlot)) {
            val candidate = when (event.action) {
                org.bukkit.event.inventory.InventoryAction.PLACE_ALL,
                org.bukkit.event.inventory.InventoryAction.PLACE_SOME,
                org.bukkit.event.inventory.InventoryAction.PLACE_ONE -> {
                    val cursor = event.cursor
                    val current = event.currentItem
                    val existing = if (current?.isSimilar(cursor) == true) current.amount else 0
                    val room = (minOf(cursor.maxStackSize, event.view.topInventory.maxStackSize) - existing).coerceAtLeast(0)
                    val placed = if (event.action == org.bukkit.event.inventory.InventoryAction.PLACE_ONE) minOf(1, room)
                        else minOf(cursor.amount, room)
                    cursor.clone().apply { amount = existing + placed }
                }
                org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR -> event.cursor
                org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP ->
                    if (event.hotbarButton in 0..8) session.player.inventory.getItem(event.hotbarButton)
                    else session.player.inventory.itemInOffHand
                else -> null
            }
            if (candidate != null && !session.acceptInput(event.rawSlot, candidate)) {
                event.isCancelled = true
                return
            }
        }
        val safe = event.action in allowedActions
        event.isCancelled = !safe || (top && !session.isInput(event.rawSlot))
        if (top && !session.isInput(event.rawSlot)) session.dispatch(event.rawSlot, event.click)
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrag(event: InventoryDragEvent) {
        val session = event.view.topInventory.holder as? MenuSession ?: return
        if (event.isCancelled) return
        session.flushInputChanges()
        if (!session.isActive || event.whoClicked.uniqueId != session.player.uniqueId ||
            event.newItems.any { (slot, item) -> slot < event.view.topInventory.size && !session.acceptInput(slot, item) } ||
            event.rawSlots.any { it < event.view.topInventory.size && (!session.isInput(it) || session.isLinked(it)) }) {
            event.isCancelled = true
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun observeClick(event: InventoryClickEvent) {
        val session = event.view.topInventory.holder as? MenuSession ?: return
        if (event.rawSlot in 0 until event.view.topInventory.size && session.isInput(event.rawSlot)) {
            session.observeInput(event.rawSlot)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun observeDrag(event: InventoryDragEvent) {
        val session = event.view.topInventory.holder as? MenuSession ?: return
        event.rawSlots.filter { it < event.view.topInventory.size && session.isInput(it) }
            .forEach { session.observeInput(it) }
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
