package dev.aethereal.benchmark

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

internal fun verifyLinkedInputs(plugin: Plugin, player: Player, done: (String) -> Unit) {
    val storage = player.inventory.storageContents.map { it?.clone() }.toTypedArray()
    val previousCursor = player.itemOnCursor.clone()
    val original = ItemStack(Material.DIAMOND, 5).apply {
        editMeta { it.lore(listOf(net.kyori.adventure.text.Component.text("Исходная строка"))) }
    }
    var extracted = false
    val menu = object : SingleGUISurface(1, plugin) {
        override fun onUpdateNodes() {
            with(newPattern("S.T.....B")) {
                inputAt(pos('S').first(), pos('T').first()) {
                    placeholder {
                        type(Material.BARRIER)
                        name("<red>Поместите предмет")
                        loreLine("<gray>ЛКМ — стак, ПКМ — один")
                    }
                    loreLine("<green>Временная строка")
                }
                iconAt(pos('B')) {
                    type(Material.ANVIL)
                    onClick {
                        check(!isInventoryFull())
                        check(giveItem(Material.EMERALD, 2))
                        check(countItem(Material.EMERALD) == 2)
                        check(hasItem(Material.EMERALD, 2))
                        check(takeItem(Material.EMERALD, 2))
                        check(countItem(Material.EMERALD) == 0)
                        check(hasPermission("adrenalingui.test") == player.hasPermission("adrenalingui.test"))
                        val item = checkNotNull(inputItem(2))
                        check(item == original)
                        item.amount = 1
                        check(inputItem(2)?.amount == 5)
                        check(takeInput(2) == original)
                        check(inputItem(2) == null)
                        check(player.openInventory.topInventory.getItem(0)?.type == Material.BARRIER)
                        check(takeInput(2) == null)
                        extracted = true
                    }
                }
            }
        }
    }
    fun click(slot: Int, click: ClickType = ClickType.LEFT, action: InventoryAction = InventoryAction.PICKUP_ALL) {
        val event = InventoryClickEvent(player.openInventory, InventoryType.SlotType.CONTAINER, slot, click, action)
        plugin.server.pluginManager.callEvent(event)
        check(event.isCancelled)
    }
    fun restore() {
        player.closeInventory()
        player.inventory.storageContents = storage
        player.setItemOnCursor(previousCursor)
    }
    try {
        player.inventory.storageContents = arrayOfNulls(storage.size)
        menu.open(player)
        player.setItemOnCursor(original.clone())
        click(0, ClickType.RIGHT)
        check(player.itemOnCursor.amount == 4)
        check(player.openInventory.topInventory.getItem(0) == null)
        val display = checkNotNull(player.openInventory.topInventory.getItem(2))
        check(display.amount == 1 && display.itemMeta.lore()?.size == 2)
        JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine.refresh(player)
        check(player.openInventory.topInventory.getItem(0) == null)
        check(player.openInventory.topInventory.getItem(2) == display)
        click(0)
        check(player.itemOnCursor.amount == 4)
        click(2)
        check(player.itemOnCursor == original)
        check(player.openInventory.topInventory.getItem(0)?.type == Material.BARRIER)
        click(0)
        check(player.itemOnCursor.isEmpty)
        check(player.openInventory.topInventory.getItem(2)?.amount == 5)
        val drag = InventoryDragEvent(player.openInventory, null, original.clone(), false, mapOf(2 to original.clone()))
        plugin.server.pluginManager.callEvent(drag)
        check(drag.isCancelled)
        click(2, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY)
        check(player.inventory.getItem(0) == original)
        check(player.openInventory.topInventory.getItem(0)?.type == Material.BARRIER)
        player.inventory.setItem(0, null)
        player.setItemOnCursor(original.clone())
        click(0)
        player.closeInventory()
        check(player.inventory.getItem(0) == original)
        check(player.inventory.storageContents.filterNotNull().sumOf { it.amount } == 5)
        player.inventory.setItem(0, null)
        menu.open(player)
        check(player.openInventory.topInventory.getItem(0)?.type == Material.BARRIER)
        check(player.openInventory.topInventory.getItem(2) == null)
        player.setItemOnCursor(original.clone())
        click(0)
        click(8)
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            try {
                check(extracted)
                player.closeInventory()
                check(player.inventory.storageContents.all { it == null || it.isEmpty })
                done("Проверка пройдена")
            } catch (exception: Throwable) {
                done("Ошибка проверки: " + exception.toString())
                plugin.logger.log(java.util.logging.Level.SEVERE, "Ошибка проверки связанных полей", exception)
            } finally {
                restore()
            }
        }, 3L)
    } catch (exception: Throwable) {
        restore()
        throw exception
    }
}
