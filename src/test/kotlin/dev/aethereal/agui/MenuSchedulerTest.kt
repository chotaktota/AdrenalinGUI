package dev.aethereal.agui

import dev.aethereal.agui.engine.MenuScheduler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MenuSchedulerTest {
    private class Stand(private val offset: Int = 0) {
        class Job(val due: Long, val action: () -> Unit, var cancelled: Boolean = false)
        var time = 0L
        val jobs = mutableListOf<Job>()
        val scheduler = MenuScheduler({ offset + time.toInt() }) { delay, action ->
            val job = Job(time + delay, action)
            jobs += job
            MenuScheduler.Handle { job.cancelled = true }
        }
        fun active() = jobs.count { !it.cancelled }
        fun advance(ticks: Int) {
            val end = time + ticks
            while (true) {
                val job = jobs.filter { !it.cancelled }.minByOrNull { it.due } ?: break
                if (job.due > end) break
                time = job.due
                job.cancelled = true
                job.action()
            }
            time = end
        }
    }
    @Test fun idleSchedulerHasNoBackgroundTask() {
        val stand = Stand()
        assertEquals(0, stand.active())
        stand.advance(100)
        assertEquals(0, stand.jobs.size)
    }
    @Test fun manyActionsShareOneWakeAndPreserveOrder() {
        val stand = Stand()
        val calls = mutableListOf<Int>()
        repeat(1000) { index -> stand.scheduler.later(10) { calls += index } }
        assertEquals(1, stand.active())
        stand.advance(9)
        assertTrue(calls.isEmpty())
        stand.advance(1)
        assertEquals((0 until 1000).toList(), calls)
        assertEquals(0, stand.active())
    }
    @Test fun earlierActionMovesWakeAndCancelledActionsNeverRun() {
        val stand = Stand()
        val calls = mutableListOf<String>()
        stand.scheduler.later(10) { calls += "позже" }
        val cancelled = stand.scheduler.later(3) { calls += "отменено" }
        cancelled.cancel()
        cancelled.cancel()
        stand.scheduler.later(5) { calls += "раньше" }
        assertEquals(1, stand.active())
        stand.advance(10)
        assertEquals(listOf("раньше", "позже"), calls)
    }
    @Test fun nestedActionCannotRunInTheCurrentTick() {
        val stand = Stand()
        val times = mutableListOf<Long>()
        stand.scheduler.later(1) {
            times += stand.time
            stand.scheduler.later(1) { times += stand.time }
        }
        stand.advance(2)
        assertEquals(listOf(1L, 2L), times)
    }
    @Test fun closeCancelsAllWorkAndRejectsNewActions() {
        val stand = Stand()
        val handle = stand.scheduler.later(1) { error("Закрытая задача не должна выполняться") }
        stand.scheduler.close()
        stand.scheduler.close()
        handle.cancel()
        stand.advance(5)
        assertEquals(0, stand.active())
        assertFailsWith<IllegalStateException> { stand.scheduler.later(1) {} }
    }
    @Test fun nativeTickCounterWrapDoesNotMoveDueTimes() {
        val stand = Stand(Int.MAX_VALUE - 2)
        var calls = 0
        stand.scheduler.later(6) { calls++ }
        stand.advance(5)
        assertEquals(0, calls)
        stand.advance(1)
        assertEquals(1, calls)
    }
    @Test fun callbackFailureDoesNotLoseOtherPendingActions() {
        val stand = Stand()
        var calls = 0
        stand.scheduler.later(1) { error("Проверка ошибки") }
        stand.scheduler.later(2) { calls++ }
        assertFailsWith<IllegalStateException> { stand.advance(1) }
        stand.advance(2)
        assertEquals(1, calls)
    }
    @Test fun invalidDelaysAreRejected() {
        val stand = Stand()
        assertFailsWith<IllegalArgumentException> { stand.scheduler.later(0) {} }
        assertFailsWith<IllegalArgumentException> { stand.scheduler.later(Long.MAX_VALUE) {} }
    }
}
