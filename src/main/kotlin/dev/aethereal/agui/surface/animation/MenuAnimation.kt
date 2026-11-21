package dev.aethereal.agui.surface.animation

import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.surface.icon.DynamicIcon
import dev.aethereal.agui.surface.icon.Icon
import org.bukkit.entity.Player

class MenuAnimation internal constructor(private val size: Int) {
    internal val steps = sortedMapOf<Long, MutableList<AnimationOperation>>()
    internal var period: Long? = null
    fun at(tick: Long, configure: AnimationFrame.() -> Unit) {
        MenuEngine.requireSync()
        require(tick >= 0) { "Тик анимации не может быть отрицательным" }
        val frame = AnimationFrame(size).apply(configure)
        steps.getOrPut(tick) { mutableListOf() }.addAll(frame.operations)
    }
    fun loop(ticks: Long) {
        MenuEngine.requireSync()
        require(ticks > 0) { "Период анимации должен быть положительным" }
        period = ticks
    }
}

class AnimationFrame internal constructor(private val size: Int) {
    internal val operations = mutableListOf<AnimationOperation>()
    private fun slots(values: Iterable<Int>): List<Int> = values.toList().distinct().also {
        require(it.isNotEmpty() && it.all { slot -> slot in 0 until size }) { "Нужно указать непустой список слотов в пределах меню" }
    }
    fun set(slots: Iterable<Int>, configure: Icon.() -> Unit) {
        MenuEngine.requireSync()
        operations += AnimationOperation.Set(slots(slots), Icon().apply(configure))
    }
    fun set(slot: Int, configure: Icon.() -> Unit) = set(listOf(slot), configure)
    fun hide(slots: Iterable<Int>) { MenuEngine.requireSync(); operations += AnimationOperation.Visibility(slots(slots), false) }
    fun hide(slot: Int) = hide(listOf(slot))
    fun show(slots: Iterable<Int>) { MenuEngine.requireSync(); operations += AnimationOperation.Visibility(slots(slots), true) }
    fun show(slot: Int) = show(listOf(slot))
    fun change(slots: Iterable<Int>, configure: DynamicIcon.(Player) -> Unit) {
        MenuEngine.requireSync()
        operations += AnimationOperation.Change(slots(slots), configure)
    }
    fun change(slot: Int, configure: DynamicIcon.(Player) -> Unit) = change(listOf(slot), configure)
}

internal sealed interface AnimationOperation {
    val slots: List<Int>
    class Set(override val slots: List<Int>, val icon: Icon) : AnimationOperation
    class Visibility(override val slots: List<Int>, val visible: Boolean) : AnimationOperation
    class Change(override val slots: List<Int>, val configure: DynamicIcon.(Player) -> Unit) : AnimationOperation
}
