package dev.aethereal.agui.menu.session

import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.menu.cache.CachedMenu
import dev.aethereal.agui.surface.icon.action.GUIClick
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import java.util.logging.Level

internal class MenuSession(
    val engine: MenuEngine,
    val owner: Plugin,
    val player: Player,
    private val template: CachedMenu,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    private val page: String? = null,
    private val onNavigate: ((String) -> Unit)? = null
) : InventoryHolder {
    private val text = engine.text.forPlayer(player)
    private val contents = owner.server.createInventory(this, template.size, template.title(text))
    private var active = false
    private var queued = false
    private val lifecycle = SessionLifecycle(
        { notify("onOpen", onOpen) }, { notify("onClose", onClose) }
    )
    init { template.populate(contents, text) }
    override fun getInventory(): Inventory = contents
    fun open() {
        engine.started(this)
        active = true
        try {
            player.openInventory(contents)
            if (active && player.openInventory.topInventory === contents) lifecycle.open()
        }
        finally { if (player.openInventory.topInventory !== contents) dispose() }
    }
    fun dispatch(slot: Int, click: ClickType) {
        val handler = template.handler(slot) ?: return
        if (queued || !active || !owner.isEnabled) return
        queued = true
        engine.plugin.server.scheduler.runTask(engine.plugin, Runnable {
            queued = false
            if (active && owner.isEnabled && player.isOnline && player.openInventory.topInventory === contents) {
                try {
                    val navigate = onNavigate?.let { action ->
                        { target: String ->
                            if (active && owner.isEnabled && player.isOnline &&
                                player.openInventory.topInventory === contents) action(target)
                        }
                    }
                    handler.invoke(GUIClick(player, slot, click, page, navigate))
                }
                catch (exception: Exception) {
                    owner.logger.log(Level.SEVERE, "Ошибка обработки клика в слоте $slot", exception)
                }
            }
        })
    }
    fun dispose() {
        active = false
        engine.finished(this)
        lifecycle.close()
    }
    fun close() {
        try {
            if (player.openInventory.topInventory === contents) player.closeInventory()
        } finally { dispose() }
    }

    private fun notify(name: String, callback: () -> Unit) {
        try { callback() }
        catch (exception: Exception) {
            owner.logger.log(Level.SEVERE, "Ошибка обработчика меню $name", exception)
        }
    }
}
