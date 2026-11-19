package dev.aethereal.agui.surface.pattern

import dev.aethereal.agui.surface.icon.Icon
import net.kyori.adventure.text.minimessage.MiniMessage
import dev.aethereal.agui.menu.MenuEngine
import net.kyori.adventure.text.Component

class Pattern internal constructor(layout: String, rows: Int) {
    private val positions: Map<Char, List<Int>>
    internal val size = rows * 9
    internal var caption: Component = Component.empty()
    internal val icons = linkedMapOf<Int, Icon>()
    init {
        val lines = layout.trimIndent().lines()
        require(lines.size == rows && lines.all { it.length == 9 }) {
            "Шаблон должен содержать $rows строк по 9 символов"
        }
        positions = lines.joinToString("").withIndex().filter { it.value != '.' }
            .groupBy({ it.value }, { it.index })
    }
    fun pos(symbol: Char): List<Int> = requireNotNull(positions[symbol]) { "Символ отсутствует в шаблоне: $symbol" }
    fun title(value: String) {
        MenuEngine.requireSync()
        caption = miniMessage.deserialize(value)
    }
    fun title(value: () -> String) = title(value())
    fun iconAt(slots: Iterable<Int>, configure: Icon.() -> Unit) {
        MenuEngine.requireSync()
        val targets = slots.toList()
        require(targets.all { it in 0 until size }) { "Слот находится за пределами меню" }
        val icon = Icon().apply(configure)
        targets.forEach { icons[it] = icon }
    }
    fun iconAt(slot: Int, configure: Icon.() -> Unit) = iconAt(listOf(slot), configure)
    private companion object {
        val miniMessage = MiniMessage.miniMessage()
    }
}
