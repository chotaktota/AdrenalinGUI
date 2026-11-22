package dev.aethereal.benchmark

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.surface.SingleGUISurface
import dev.aethereal.agui.surface.item.InputCause
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

internal fun verifyInputRules(plugin: Plugin, player: Player, done: (String) -> Unit) {
    val original = player.inventory.storageContents.map { it?.clone() }.toTypedArray()
    val cursor = player.itemOnCursor.clone()
    val events = mutableListOf<String>()
    var enabled = true
    var executed = 0
    var denied = 0
    var extract = false
    var stopped = false
    val menu = object : SingleGUISurface(1, plugin) {
        override fun onUpdateNodes() {
            with(newPattern("X.S.T.B..")) {
                inputAt(0) {
                    acceptIf { item -> item.type == Material.DIAMOND && item.amount <= 8 }
                    onInsert { change ->
                        events += "I0:${change.item?.amount}"
                        change.current?.amount = 99
                    }
                    onTake { change -> events += "T0:${change.item?.amount}" }
                    onChange { change -> events += "C0:${change.current?.amount ?: 0}" }
                }
                inputAt(2, 4) {
                    acceptIf { item ->
                        val allowed = item.type == Material.DIAMOND
                        item.amount = 99
                        allowed
                    }
                    onInsert { change -> events += "I4:${change.item?.amount}" }
                    onTake { change ->
                        check(change.cause == InputCause.TAKE_INPUT)
                        events += "T4:${change.item?.amount}"
                    }
                    onChange { change -> events += "C4:${change.current?.amount ?: 0}" }
                }
                iconAt(7) {
                    type(Material.STONE)
                    enabledIf("Нет предмета") { viewer ->
                        check(viewer.openInventory.topInventory.size == 9)
                        viewer.openInventory.topInventory.getItem(8)?.isEmpty == false
                    }
                }
                iconAt(6) {
                    type(Material.ANVIL)
                    name("Готово")
                    enabledIf("<red>Нужно разрешение") { enabled }
                    disabled { reason ->
                        name("<gray>Недоступно")
                        lore(reason)
                    }
                    onDenied {
                        denied++
                        sound(Sound.ENTITY_VILLAGER_NO)
                    }
                    onClick {
                        executed++
                        sound(Sound.UI_BUTTON_CLICK)
                        sound("minecraft:ui.button.click", pitch = 1.2f)
                        check(runCatching { sound(Sound.UI_BUTTON_CLICK, volume = Float.NaN) }.isFailure)
                        if (extract) check(takeInput(4)?.amount == 1)
                    }
                }
            }
        }
    }
    fun click(slot: Int, type: ClickType = ClickType.LEFT, action: InventoryAction = InventoryAction.PICKUP_ALL, hotbar: Int = -1): InventoryClickEvent {
        val event = InventoryClickEvent(player.openInventory, InventoryType.SlotType.CONTAINER, slot, type, action, hotbar)
        plugin.server.pluginManager.callEvent(event)
        return event
    }
    fun finish(result: String) {
        if (stopped) return
        stopped = true
        player.closeInventory()
        player.inventory.storageContents = original
        player.setItemOnCursor(cursor)
        done(result)
    }
    fun later(ticks: Long, action: () -> Unit) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (!stopped) {
                try { action() }
                catch (exception: Throwable) {
                    plugin.logger.log(java.util.logging.Level.SEVERE, "Ошибка проверки условий и событий полей", exception)
                    finish("Ошибка проверки: " + exception.toString())
                }
            }
        }, ticks)
    }
    try {
        player.inventory.storageContents = arrayOfNulls(original.size)
        player.setItemOnCursor(null)
        menu.open(player)
        player.inventory.setItem(0, ItemStack(Material.EMERALD, 5))
        check(click(36, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled)
        check(player.openInventory.topInventory.getItem(0) == null && player.inventory.getItem(0)?.amount == 5)
        player.inventory.setItem(0, ItemStack(Material.DIAMOND, 5))
        click(36, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY)
        check(player.openInventory.topInventory.getItem(0)?.amount == 5)
        check(player.openInventory.topInventory.getItem(4) == null)
        enabled = false
        click(6)
        later(4) {
            check(executed == 0 && denied == 1)
            check(player.openInventory.topInventory.getItem(6)?.itemMeta?.customName() ==
                net.kyori.adventure.text.Component.text("Недоступно", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
            check(events == listOf("I0:5", "C0:5"))
            check(player.openInventory.topInventory.getItem(0)?.amount == 5)
            player.setItemOnCursor(ItemStack(Material.EMERALD))
            check(click(0, action = InventoryAction.SWAP_WITH_CURSOR).isCancelled)
            player.setItemOnCursor(ItemStack(Material.DIAMOND))
            check(!click(0, action = InventoryAction.PLACE_ONE).isCancelled)
            player.openInventory.topInventory.setItem(0, ItemStack(Material.DIAMOND, 6))
            player.setItemOnCursor(null)
            val drag = InventoryDragEvent(player.openInventory, null, ItemStack(Material.EMERALD), false, mapOf(0 to ItemStack(Material.EMERALD)))
            plugin.server.pluginManager.callEvent(drag)
            check(drag.isCancelled)
            player.inventory.setItem(8, ItemStack(Material.EMERALD))
            check(click(0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 8).isCancelled)
            player.setItemOnCursor(ItemStack(Material.EMERALD, 3))
            click(2)
            check(player.itemOnCursor.amount == 3 && player.openInventory.topInventory.getItem(4) == null)
            player.setItemOnCursor(ItemStack(Material.DIAMOND, 3))
            click(2, ClickType.RIGHT)
            check(player.itemOnCursor.amount == 2 && player.openInventory.topInventory.getItem(4)?.amount == 1)
            enabled = true
            JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine.refresh(player)
            extract = true
            click(6)
        }
        later(8) {
            check(executed == 1 && denied == 1)
            check(events == listOf("I0:5", "C0:5", "I0:1", "C0:6", "I4:1", "C4:1", "T4:1", "C4:0"))
            check(player.openInventory.topInventory.getItem(2)?.type == Material.BARRIER)
            val count = events.size
            player.setItemOnCursor(ItemStack(Material.DIAMOND))
            check(!click(0, action = InventoryAction.PLACE_ONE).isCancelled)
            player.openInventory.topInventory.setItem(0, ItemStack(Material.DIAMOND, 7))
            player.setItemOnCursor(null)
            player.closeInventory()
            later(3) {
                check(events.size == count + 2)
                check(events.takeLast(2) == listOf("I0:1", "C0:7"))
                check(events.none { it.startsWith("T0:") })
                finish("Проверка пройдена")
            }
        }
    } catch (exception: Throwable) {
        finish("Ошибка проверки: " + exception.toString())
        throw exception
    }
}
