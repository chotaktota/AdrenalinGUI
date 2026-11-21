package dev.aethereal.agui.menu.session

import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.menu.cache.CachedMenu
import dev.aethereal.agui.surface.GUIClick
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
    private val animated = template.animation?.let { template.animatedContents() }
    private val cursor = template.animation?.cursor()
    private var animationTask: dev.aethereal.agui.menu.task.MenuScheduler.Handle? = null
    private var active = false
    private var queued = false
    private val lifecycle = SessionLifecycle(
        { notify("onOpen", onOpen) }, { notify("onClose", onClose) }
    )
    init {
        template.populate(contents, text)
    }
    override fun getInventory(): Inventory = contents
    fun open() {
        engine.started(this)
        active = true
        try {
            player.openInventory(contents)
            if (active && player.openInventory.topInventory === contents) {
                lifecycle.open()
                if (active && player.openInventory.topInventory === contents) nextFrame()
            }
        }
        finally { if (player.openInventory.topInventory !== contents) dispose() }
    }
    fun refresh(slots: IntArray) {
        MenuEngine.requireSync()
        if (!active || player.openInventory.topInventory !== contents) return
        val context = engine.text.forPlayer(player)
        if (animated == null) template.refresh(contents, context, slots) else animated.refresh(contents, context, slots)
    }
    fun dispatch(slot: Int, click: ClickType) {
        val handler = (if (animated == null) template.handler(slot) else animated.handler(slot)) ?: return
        val revision = animated?.revision(slot)
        if (queued || !active || !owner.isEnabled) return
        queued = true
        engine.tasks.later(1L) {
            queued = false
            if (active && owner.isEnabled && player.isOnline && player.openInventory.topInventory === contents) {
                if (animated?.revision(slot) != revision) return@later
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
        }
    }
    private fun nextFrame() {
        val next = cursor?.next() ?: return
        if (next.first == 0L) {
            runFrame(next.second)
            return
        }
        animationTask = engine.tasks.later(next.first) {
            animationTask = null
            runFrame(next.second)
        }
    }
    private fun runFrame(operations: List<dev.aethereal.agui.menu.cache.CachedAnimation.Operation>) {
        if (!active || !owner.isEnabled || !player.isOnline || player.openInventory.topInventory !== contents) return
        try {
            val slots = checkNotNull(animated).apply(operations)
            refresh(slots)
            if (active && player.openInventory.topInventory === contents) nextFrame()
        } catch (exception: Exception) {
            animationTask?.cancel()
            animationTask = null
            owner.logger.log(Level.SEVERE, "Ошибка анимации меню", exception)
            close()
        }
    }
    fun dispose() {
        active = false
        animationTask?.cancel()
        animationTask = null
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
