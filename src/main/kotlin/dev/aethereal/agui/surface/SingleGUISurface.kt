package dev.aethereal.agui.surface

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.menu.cache.CachedMenu
import dev.aethereal.agui.menu.cache.MenuCache
import dev.aethereal.agui.menu.MenuEngine
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
abstract class SingleGUISurface(val rows: Int, final override val plugin: Plugin) : GUISurface {
    private val cache = MenuCache<CachedMenu>()
    private var building: Pattern? = null
    private var constructing = false
    init { require(rows in 1..6) { "Количество строк меню должно быть от 1 до 6" } }

    protected abstract fun onUpdateNodes()
    protected open fun onOpen(player: Player) {}
    protected open fun onClose(player: Player) {}
    final override fun invalidateCache() {
        MenuEngine.requireSync()
        check(!constructing) { "Нельзя сбросить кэш во время построения меню" }
        cache.clear()
    }

    protected fun newPattern(layout: String): Pattern {
        MenuEngine.requireSync()
        check(constructing && building == null) { "Внутри onUpdateNodes необходимо создать ровно один шаблон" }
        return Pattern(layout, rows).also { building = it }
    }

    final override fun open(player: Player) {
        MenuEngine.requireSync()
        check(plugin.isEnabled) { "Плагин-владелец меню отключён" }
        val runtime = JavaPlugin.getPlugin(AdrenalinGUI::class.java)
        check(runtime.isEnabled) { "AdrenalinGUI отключён" }
        check(!constructing) { "Обнаружено рекурсивное построение меню" }
        val template = cache.getOrBuild {
            constructing = true
            try {
                onUpdateNodes()
                CachedMenu(checkNotNull(building) { "Метод onUpdateNodes должен создать шаблон" })
            } finally { building = null; constructing = false }
        }
        runtime.engine.open(this, player, template,
            onOpen = { onOpen(player) }, onClose = { onClose(player) })
    }
}
