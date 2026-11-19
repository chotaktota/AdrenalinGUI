package dev.aethereal.agui.menu.page

import dev.aethereal.agui.surface.pattern.Pattern
import dev.aethereal.agui.menu.MenuEngine

class PageManager internal constructor(private val id: String, private val rows: Int) {
    private var pattern: Pattern? = null

    fun newPattern(layout: String): Pattern {
        MenuEngine.requireSync()
        check(pattern == null) { "Страница должна содержать ровно один шаблон" }
        return Pattern(layout, rows).also { pattern = it }
    }

    internal fun build(): MenuPage = MenuPage(id,
        checkNotNull(pattern) { "Для страницы необходимо создать шаблон" })
}
