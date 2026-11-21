package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.engine.session.AnimationCursor
import dev.aethereal.agui.surface.animation.AnimationOperation
import dev.aethereal.agui.surface.animation.MenuAnimation
import dev.aethereal.agui.surface.item.Icon

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
        class Change(override val slots: List<Int>, val configure: dev.aethereal.agui.surface.item.DynamicIcon.(org.bukkit.entity.Player) -> Unit) : Operation
    }
}
