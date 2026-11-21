package dev.aethereal.benchmark

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.surface.SingleGUISurface
import dev.aethereal.agui.surface.MultiGUISurface
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

internal class AnimationVerification(private val plugin: JavaPlugin, private val viewer: Player, private val status: (String) -> Unit) {
    private val engine = (plugin.server.pluginManager.getPlugin("AdrenalinGUI") as AdrenalinGUI).engine
    private var edits = 0
    private var opened = 0
    private var closed = 0
    private var failed = false
    private val other = checkNotNull(plugin.server.getPlayerExact("Bench1"))
    private val menu = object : SingleGUISurface(1, plugin) {
        override fun onOpen(player: Player) { opened++ }
        override fun onClose(player: Player) { closed++ }
        override fun onUpdateNodes() {
            with(newPattern("X........")) {
                title("Анимация")
                iconAt(0) { type(Material.PAPER); name("<white>Начало"); onClick { close() } }
                animation {
                    at(0) { hide(0) }
                    at(2) { show(0) }
                    at(4) { change(0) { player -> edits++; name("<gold>${player.name}"); glow() } }
                    at(6) { set(0) { type(Material.EMERALD); name("<green>Готово"); onClick { close() } } }
                    at(8) { hide(0) }
                    at(10) { show(0) }
                    loop(12)
                }
            }
        }
    }
    fun start() {
        menu.open(viewer)
        val inventory = viewer.openInventory.topInventory
        check(inventory.getItem(0) == null)
        later(3) {
            check(inventory.getItem(0)?.type == Material.PAPER)
            engine.refresh(viewer)
            check(inventory.getItem(0)?.type == Material.PAPER)
        }
        later(5) {
            val item = checkNotNull(inventory.getItem(0))
            check(item.itemMeta.enchantmentGlintOverride == true)
            val snapshot = item.clone()
            engine.refresh(viewer, 0)
            check(inventory.getItem(0) == snapshot)
            menu.open(other)
            check(other.openInventory.topInventory.getItem(0) == null)
        }
        later(7) { check(inventory.getItem(0)?.type == Material.EMERALD) }
        later(8) { check(other.openInventory.topInventory.getItem(0)?.type == Material.PAPER) }
        later(9) { engine.refresh(viewer); check(inventory.getItem(0) == null) }
        later(11) { check(inventory.getItem(0)?.type == Material.EMERALD) }
        later(13) {
            check(inventory.getItem(0) == null)
            check(opened == 2 && closed == 0)
            viewer.closeInventory()
            val secondInventory = other.openInventory.topInventory
            val secondSnapshot = secondInventory.contents.map { it?.clone() }
            other.closeInventory()
            val count = edits
            later(8) {
                check(edits == count)
                check(inventory.getItem(0) == null)
                check(closed == 2)
                check(secondInventory.contents.toList() == secondSnapshot)
                verifyClicks()
            }
        }
    }
    private fun verifyClicks() {
        var original = 0
        var replacement = 0
        val clicks = object : SingleGUISurface(1, plugin) {
            override fun onUpdateNodes() {
                with(newPattern("X........")) {
                    iconAt(0) { type(Material.PAPER); onClick { original++ } }
                    animation {
                        at(1) { hide(0) }
                        at(4) { set(0) { type(Material.EMERALD); onClick { replacement++ } } }
                    }
                }
            }
        }
        fun click() {
            plugin.server.pluginManager.callEvent(org.bukkit.event.inventory.InventoryClickEvent(
                viewer.openInventory, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 0,
                org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL))
        }
        clicks.open(viewer)
        click()
        later(3) { check(original == 0); click() }
        later(5) { check(original == 0 && replacement == 0); click() }
        later(7) {
            check(original == 0 && replacement == 1)
            viewer.closeInventory()
            verifyPages()
        }
    }

    private fun verifyPages() {
        var obsolete = 0
        val multi = object : MultiGUISurface(1, plugin) {
            override fun onUpdateNodes() {
                page("main") {
                    with(newPattern("X........")) {
                        iconAt(0) { type(Material.PAPER) }
                        animation { at(4) { change(0) { obsolete++; name("Старое окно") } }; loop(8) }
                    }
                }
                page("next") { with(newPattern("X........")) { iconAt(0) { type(Material.EMERALD) } } }
            }
        }
        multi.open(viewer)
        later(1) { multi.open(viewer, "next") }
        later(7) {
            check(obsolete == 0)
            check(viewer.openInventory.topInventory.getItem(0)?.type == Material.EMERALD)
            viewer.closeInventory()
            status("Проверка анимации пройдена")
        }
    }
    private fun later(delay: Long, action: () -> Unit) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (!failed) try { action() } catch (exception: Exception) {
                failed = true
                status("Ошибка: ${exception.message}")
                plugin.logger.log(java.util.logging.Level.SEVERE, "Ошибка проверки анимации", exception)
                viewer.closeInventory()
            }
        }, delay)
    }
}
