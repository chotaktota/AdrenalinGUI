package dev.aethereal.agui.surface.layout

import dev.aethereal.agui.engine.cache.MenuCache

internal class PageRegistry<T : Any> {
    private class Entry<T : Any>(val build: () -> T) {
        val cache = MenuCache<T>()
    }

    private val entries = linkedMapOf<String, Entry<T>>()

    fun register(id: String, build: () -> T) {
        require(id.isNotBlank()) { "Название страницы не может быть пустым" }
        require(id !in entries) { "Страница уже зарегистрирована: $id" }
        entries[id] = Entry(build)
    }

    fun get(id: String): T {
        val entry = requireNotNull(entries[id]) { "Страница не найдена: $id" }
        return entry.cache.getOrBuild(entry.build)
    }

    fun invalidate(id: String) {
        requireNotNull(entries[id]) { "Страница не найдена: $id" }.cache.clear()
    }
}
