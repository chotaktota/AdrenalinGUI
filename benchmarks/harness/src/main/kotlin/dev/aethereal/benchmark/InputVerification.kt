package dev.aethereal.benchmark

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.plugin.Plugin

internal fun verifyInputs(plugin: Plugin, player: Player, done: (String) -> Unit) {
    val original = player.inventory.storageContents.map { it?.clone() }.toTypedArray()
    var clicks = 0
    val menu = object : SingleGUISurface(1, plugin) {
        override fun onUpdateNodes() {
            with(newPattern("X......BB")) {
                inputAt(pos('X'))
                iconAt(pos('B')) {
                    type(Material.STONE)
                    cooldown(10)
                    onClick { clicks++; refresh() }
                }
            }
        }
    }
    fun click(slot: Int, action: InventoryAction = InventoryAction.PICKUP_ALL): InventoryClickEvent {
        val event = InventoryClickEvent(player.openInventory, InventoryType.SlotType.CONTAINER,
            slot, if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) ClickType.SHIFT_LEFT else ClickType.LEFT, action)
        plugin.server.pluginManager.callEvent(event)
        return event
    }
    fun later(ticks: Long, action: () -> Unit) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            try { action() }
            catch (error: Throwable) {
                player.closeInventory()
                player.inventory.storageContents = original
                done("Ошибка: " + error.toString())
                plugin.logger.log(java.util.logging.Level.SEVERE, "Ошибка проверки полей", error)
            }
        }, ticks)
    }
    player.inventory.storageContents = arrayOfNulls(original.size)
    player.inventory.setItem(0, org.bukkit.inventory.ItemStack(Material.DIAMOND, 12))
    menu.open(player)
    check(click(36, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled)
    check(player.openInventory.topInventory.getItem(0)?.amount == 12)
    check(player.inventory.getItem(0) == null)
    check(!click(0).isCancelled)
    check(click(1).isCancelled)
    click(7)
    later(3) {
        check(clicks == 1)
        check(player.openInventory.topInventory.getItem(0)?.amount == 12)
        click(8)
    }
    later(6) { check(clicks == 1) }
    later(13) { click(8) }
    later(16) {
        check(clicks == 2)
        player.closeInventory()
        check(player.inventory.storageContents.filterNotNull().sumOf { if (it.type == Material.DIAMOND) it.amount else 0 } == 12)
        check(player.inventory.storageContents.filterNotNull().none { it.type == Material.STONE })
        player.inventory.storageContents = original
        verifyLinkedInputs(plugin, player, done)
    }
}
