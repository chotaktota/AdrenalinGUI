package dev.aethereal.agui.engine

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.engine.cache.CachedMenu
import dev.aethereal.agui.engine.session.MenuSession
import dev.aethereal.agui.text.TextRenderer
import dev.aethereal.agui.surface.GUISurface
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class MenuEngine internal constructor(val plugin: AdrenalinGUI) : AutoCloseable {
    val menus = MenuRegistry()
    private val economyAccess by lazy { dev.aethereal.agui.integration.MenuEconomy(plugin) }
    private val permissionAccess by lazy { dev.aethereal.agui.integration.MenuPermissions(plugin) }
    val economy: dev.aethereal.agui.integration.MenuEconomy
        get() { requireSync(); checkAvailable(); check(plugin.server.pluginManager.isPluginEnabled("Vault")) { "Vault недоступен" }; return economyAccess }
    val permissions: dev.aethereal.agui.integration.MenuPermissions
        get() { requireSync(); checkAvailable(); check(plugin.server.pluginManager.isPluginEnabled("LuckPerms")) { "LuckPerms недоступен" }; return permissionAccess }
    internal val text = TextRenderer(plugin)
    internal val tasks = dev.aethereal.agui.engine.MenuScheduler(org.bukkit.Bukkit::getCurrentTick) { delay, action ->
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable { action() }, delay)
        dev.aethereal.agui.engine.MenuScheduler.Handle { task.cancel() }
    }
    private val sessions = linkedSetOf<MenuSession>()
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
        MenuSession(this, surface.plugin, player, template, onOpen, onClose, page, onNavigate).open()
    }

    fun refresh(player: Player, vararg slots: Int) {
        requireSync()
        checkAvailable()
        val session = player.openInventory.topInventory.holder as? MenuSession ?: return
        if (session.engine !== this || session.player.uniqueId != player.uniqueId) return
        session.refresh(if (slots.isEmpty()) IntArray(session.inventory.size) { it } else slots)
    }

    internal fun started(session: MenuSession) { sessions.add(session) }
    internal fun finished(session: MenuSession) { sessions.remove(session) }

    internal fun close(owner: Plugin) {
        requireSync()
        if (owner === plugin) { close(); return }
        sessions.filter { it.owner === owner }.forEach { it.close() }
        menus.clear(owner)
    }

    override fun close() {
        requireSync()
        if (closed) return
        closed = true
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
