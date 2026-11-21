package dev.aethereal.agui.menu.cache

import dev.aethereal.agui.menu.session.AnimationCursor
import dev.aethereal.agui.surface.AnimationOperation
import dev.aethereal.agui.surface.MenuAnimation
import dev.aethereal.agui.surface.Icon

internal class CachedAnimation(animation: MenuAnimation, compile: (Icon) -> CachedNode) {
    val frames = animation.steps.map { (tick, operations) -> tick to operations.map { operation ->
        when (operation) {
            is AnimationOperation.Set -> Operation.Set(operation.slots, compile(operation.icon))
            is AnimationOperation.Visibility -> Operation.Visibility(operation.slots, operation.visible)
            is AnimationOperation.Change -> Operation.Change(operation.slots, operation.configure)
        }
    } }
    val period = animation.period
    init { AnimationCursor(frames, period) }
    fun cursor() = AnimationCursor(frames, period)
    sealed interface Operation {
        val slots: List<Int>
        class Set(override val slots: List<Int>, val node: CachedNode) : Operation
        class Visibility(override val slots: List<Int>, val visible: Boolean) : Operation
        class Change(override val slots: List<Int>, val configure: dev.aethereal.agui.surface.DynamicIcon.(org.bukkit.entity.Player) -> Unit) : Operation
    }
}
