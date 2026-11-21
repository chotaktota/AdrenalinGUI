package dev.aethereal.agui.command

internal class MenuListPage<T>(entries: List<T>, requested: Int, size: Int = 45) {
    init { require(size > 0) { "Размер страницы должен быть положительным" } }
    val count = if (entries.isEmpty()) 1 else (entries.size - 1) / size + 1
    val index = requested.coerceIn(0, count - 1)
    val entries = entries.subList(index * size, minOf(entries.size.toLong(), index.toLong() * size + size).toInt()).toList()
}
