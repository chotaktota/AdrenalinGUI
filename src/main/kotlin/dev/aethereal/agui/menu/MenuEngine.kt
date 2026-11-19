package dev.aethereal.agui.menu

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.menu.cache.CachedMenu
import dev.aethereal.agui.menu.session.MenuSession
import dev.aethereal.agui.surface.GUISurface
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class MenuEngine internal constructor(val plugin: AdrenalinGUI) : AutoCloseable {
    val menus = MenuRegistry()
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

    internal fun started(session: MenuSession) { sessions.add(session) }
    internal fun finished(session: MenuSession) { sessions.remove(session) }

    internal fun close(owner: Plugin) {
        requireSync()
        sessions.filter { it.owner === owner }.forEach { it.close() }
        menus.clear(owner)
    }

    override fun close() {
        requireSync()
        if (closed) return
        closed = true
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
