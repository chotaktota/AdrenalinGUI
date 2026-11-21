package dev.aethereal.agui.surface.layout

import dev.aethereal.agui.surface.animation.MenuAnimation
import dev.aethereal.agui.surface.item.Icon
import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.text.MenuText

class Pattern internal constructor(layout: String, rows: Int) {
    private val positions: Map<Char, List<Int>>
    internal val size = rows * 9
    internal var caption = MenuText("")
    internal var animation: MenuAnimation? = null
    fun animation(configure: MenuAnimation.() -> Unit) {
        MenuEngine.requireSync()
        check(animation == null) { "Для шаблона можно задать только одну анимацию" }
        animation = MenuAnimation(size).apply(configure)
    }
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
        caption = MenuText(value)
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
}
