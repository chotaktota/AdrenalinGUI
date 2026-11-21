package dev.aethereal.agui.menu.task

import java.util.TreeSet

internal class MenuScheduler(
    private val clock: () -> Int,
    private val schedule: (Long, () -> Unit) -> Handle
) : AutoCloseable {
    class Handle(private val action: () -> Unit) {
        fun cancel() = action()
    }
    private class Work(val due: Long, val order: Long, var action: (() -> Unit)?)
    private val pending = TreeSet<Work>(compareBy<Work> { it.due }.thenBy { it.order })
    private var previousTick = clock()
    private var elapsed = 0L
    private var sequence = 0L
    private var wake: Handle? = null
    private var wakeDue: Long? = null
    private var draining = false
    private var closed = false

    fun later(delay: Long, action: () -> Unit): Handle {
        check(!closed) { "Планировщик меню закрыт" }
        require(delay in 1..Int.MAX_VALUE.toLong()) { "Задержка должна быть от 1 до 2147483647 тиков" }
        val work = Work(now() + delay, sequence++, action)
        pending.add(work)
        arm()
        return Handle {
            if (pending.remove(work)) {
                work.action = null
                arm()
            }
        }
    }
    private fun now(): Long {
        val current = clock()
        elapsed += (current - previousTick).toUInt().toLong()
        previousTick = current
        return elapsed
    }
    private fun arm() {
        if (closed || draining) return
        val next = pending.firstOrNull()?.due
        if (next == wakeDue) return
        wake?.cancel()
        wake = null
        wakeDue = next
        if (next != null) wake = schedule((next - now()).coerceAtLeast(1)) { runDue() }
    }
    private fun runDue() {
        wake = null
        wakeDue = null
        draining = true
        try {
            while (!closed) {
                val next = pending.firstOrNull() ?: break
                if (next.due > now()) break
                pending.remove(next)
                val action = next.action
                next.action = null
                action?.invoke()
            }
        } finally {
            draining = false
            arm()
        }
    }
    override fun close() {
        if (closed) return
        closed = true
        wake?.cancel()
        wake = null
        wakeDue = null
        pending.forEach { it.action = null }
        pending.clear()
    }
}
