package dev.aethereal.agui.menu

import dev.aethereal.agui.surface.GUISurface
import org.bukkit.plugin.Plugin
import java.util.WeakHashMap

class MenuRegistry internal constructor() {
    private val registered = linkedMapOf<String, GUISurface>()
    private val cached = WeakHashMap<GUISurface, Unit>()

    fun register(id: String, menu: GUISurface) {
        MenuEngine.requireSync()
        require(id.isNotBlank()) { "Название меню не может быть пустым" }
        require(id !in registered) { "Меню уже зарегистрировано: $id" }
        check(menu.plugin.isEnabled) { "Плагин-владелец меню отключён" }
        registered[id] = menu
    }

    fun find(id: String): GUISurface? {
        MenuEngine.requireSync()
        return registered[id]
    }

    fun entries(): Map<String, GUISurface> {
        MenuEngine.requireSync()
        return registered.toMap()
    }

    fun unregister(id: String): GUISurface? {
        MenuEngine.requireSync()
        return registered.remove(id)?.also { it.invalidateCache(); cached.remove(it) }
    }

    internal fun track(menu: GUISurface) { cached[menu] = Unit }

    internal fun clear(owner: Plugin? = null) {
        val menus = (registered.values + cached.keys).filter { owner == null || it.plugin === owner }.toSet()
        registered.entries.removeIf { owner == null || it.value.plugin === owner }
        menus.forEach { it.invalidateCache(); cached.remove(it) }
    }
}
