package dev.aethereal.agui.surface

import dev.aethereal.agui.surface.layout.PageManager
import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.engine.cache.CachedMenu
import dev.aethereal.agui.engine.cache.MenuCache
import dev.aethereal.agui.surface.layout.PageRegistry
import dev.aethereal.agui.engine.MenuEngine
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

abstract class MultiGUISurface(
    val rows: Int,
    final override val plugin: Plugin,
    val initialPage: String = "main"
) : GUISurface {
    private val pages = MenuCache<PageRegistry<CachedMenu>>()
    private var registering: PageRegistry<CachedMenu>? = null
    private var constructing = false

    init {
        require(rows in 1..6) { "Количество строк меню должно быть от 1 до 6" }
        require(initialPage.isNotBlank()) { "Название начальной страницы не может быть пустым" }
    }

    protected abstract fun onUpdateNodes()
    protected open fun onOpen(player: Player, page: String) {}
    protected open fun onClose(player: Player, page: String) {}

    protected fun page(id: String, rows: Int = this.rows, configure: PageManager.() -> Unit) {
        MenuEngine.requireSync()
        require(rows in 1..6) { "Количество строк страницы должно быть от 1 до 6" }
        val registry = checkNotNull(registering) { "Страницы необходимо объявлять внутри onUpdateNodes" }
        registry.register(id) { CachedMenu(PageManager(id, rows).apply(configure).build().pattern) }
    }

    final override fun invalidateCache() {
        MenuEngine.requireSync()
        check(!constructing) { "Нельзя сбросить кэш во время построения меню" }
        pages.clear()
    }

    fun invalidateCache(page: String) {
        MenuEngine.requireSync()
        registry { it.invalidate(page) }
    }

    final override fun open(player: Player) = open(player, initialPage)

    fun open(player: Player, page: String) {
        MenuEngine.requireSync()
        check(plugin.isEnabled) { "Плагин-владелец меню отключён" }
        val runtime = JavaPlugin.getPlugin(AdrenalinGUI::class.java)
        check(runtime.isEnabled) { "AdrenalinGUI отключён" }
        val template = registry { it.get(page) }
        runtime.engine.open(this, player, template,
            onOpen = { onOpen(player, page) },
            onClose = { onClose(player, page) },
            page = page,
            onNavigate = { open(player, it) }
        )
    }

    private fun <T> registry(action: (PageRegistry<CachedMenu>) -> T): T {
        check(!constructing) { "Обнаружено рекурсивное построение меню" }
        constructing = true
        return try {
            val registry = pages.getOrBuild {
                val result = PageRegistry<CachedMenu>()
                registering = result
                try { onUpdateNodes() } finally { registering = null }
                result
            }
            action(registry)
        } finally { constructing = false }
    }
}
