package dev.aethereal.agui.engine

import dev.aethereal.agui.surface.item.click.action.PlayerAction

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.engine.cache.CachedMenu
import dev.aethereal.agui.engine.session.MenuSession
import dev.aethereal.agui.engine.session.MenuDestination
import dev.aethereal.agui.engine.session.ChatInputs
import dev.aethereal.agui.text.TextUtility
import dev.aethereal.agui.surface.GUISurface
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.IdentityHashMap

class MenuEngine internal constructor(val plugin: AdrenalinGUI) : AutoCloseable {
    val menus = MenuRegistry()
    private val playerActions by lazy { PlayerAction(plugin) }
    fun actions(player: Player): PlayerAction {
        requireSync()
        checkAvailable()
        return PlayerAction(plugin, player)
    }
    val economy: PlayerAction
        get() { requireSync(); checkAvailable(); check(plugin.server.pluginManager.isPluginEnabled("Vault")) { "Vault недоступен" }; return playerActions }
    val permissions: PlayerAction
        get() { requireSync(); checkAvailable(); check(plugin.server.pluginManager.isPluginEnabled("LuckPerms")) { "LuckPerms недоступен" }; return playerActions }
    internal val text = TextUtility(plugin)
    internal val tasks = dev.aethereal.agui.engine.MenuScheduler(org.bukkit.Bukkit::getCurrentTick) { delay, action ->
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable { action() }, delay)
        dev.aethereal.agui.engine.MenuScheduler.Handle { task.cancel() }
    }
    internal val chatInputs = ChatInputs(tasks) { action -> plugin.server.scheduler.runTask(plugin, Runnable { action() }) }
    private val sessions = linkedSetOf<MenuSession>()
    private val renderingHistory = IdentityHashMap<Player, List<MenuDestination>>()
    private var closed = false

    fun open(id: String, player: Player) {
        requireSync()
        checkAvailable()
        requireNotNull(menus.find(id)) { "Меню не найдено: $id" }.open(player)
    }

    internal fun open(
        surface: GUISurface, player: Player, template: CachedMenu,
        onOpen: () -> Unit, onClose: () -> Unit,
        page: String? = null, onNavigate: ((String) -> Unit)? = null
    ) {
        requireSync()
        checkAvailable()
        check(surface.plugin.isEnabled) { "Плагин-владелец меню отключён" }
        menus.track(surface)
        val current = currentSession(player)
        val history = if (current == null) emptyList()
            else if (current.destination.surface === surface && current.destination.page == page) current.history
            else (current.history + current.destination).takeLast(32)
        val destination = MenuDestination(surface, template, onOpen, onClose, page, onNavigate)
        createSession(player, destination, history).open()
    }

    fun hasPreviousMenu(player: Player): Boolean {
        requireSync()
        val history = renderingHistory[player] ?: currentSession(player)?.history
        return history?.any { it.surface.plugin.isEnabled } == true
    }

    fun back(player: Player): Boolean {
        requireSync()
        checkAvailable()
        val current = currentSession(player) ?: return false
        val history = current.history
        val index = history.indexOfLast { it.surface.plugin.isEnabled }
        if (index < 0) return false
        val session = createSession(player, history[index], history.take(index))
        session.open()
        return currentSession(player) === session
    }

    private fun createSession(player: Player, destination: MenuDestination, history: List<MenuDestination>): MenuSession {
        val previous = renderingHistory.put(player, history)
        return try {
            MenuSession(this, player, destination, history)
        } finally {
            if (previous == null) renderingHistory.remove(player)
            else renderingHistory[player] = previous
        }
    }

    internal fun currentSession(player: Player): MenuSession? =
        (player.openInventory.topInventory.holder as? MenuSession)?.takeIf { it.engine === this && it.player === player && it.isActive }

    fun refresh(player: Player, vararg slots: Int) {
        requireSync()
        checkAvailable()
        val session = currentSession(player) ?: return
        session.refresh(if (slots.isEmpty()) IntArray(session.inventory.size) { it } else slots)
    }

    internal fun started(session: MenuSession) { sessions.add(session) }
    internal fun finished(session: MenuSession) { sessions.remove(session) }

    internal fun close(owner: Plugin) {
        requireSync()
        if (owner === plugin) { close(); return }
        chatInputs.clear(owner)
        sessions.filter { it.owner === owner }.forEach { it.close() }
        sessions.forEach { it.forget(owner) }
        menus.clear(owner)
    }

    override fun close() {
        requireSync()
        if (closed) return
        closed = true
        chatInputs.clear()
        tasks.close()
        sessions.toList().forEach { it.close() }
        sessions.clear()
        menus.clear()
    }

    private fun checkAvailable() {
        check(!closed && plugin.isEnabled) { "Движок меню отключён" }
    }

    companion object {
        fun requireSync() {
            check(org.bukkit.Bukkit.isPrimaryThread()) { "Этот вызов разрешён только на основном потоке сервера" }
        }
    }
}
