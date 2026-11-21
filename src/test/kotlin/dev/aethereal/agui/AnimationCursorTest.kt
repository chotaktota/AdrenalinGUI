package dev.aethereal.agui

import dev.aethereal.agui.menu.session.AnimationCursor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AnimationCursorTest {
    @Test fun sparseStepsUseRelativeTickDelaysAndFinish() {
        val cursor = AnimationCursor(listOf(0L to "первый", 10L to "второй", 25L to "третий"), null)
        assertEquals(0L to "первый", cursor.next())
        assertEquals(10L to "второй", cursor.next())
        assertEquals(15L to "третий", cursor.next())
        assertNull(cursor.next())
        assertNull(cursor.next())
    }
    @Test fun delayedFirstStepIsRepeatedAtTheSameCycleOffset() {
        val cursor = AnimationCursor(listOf(5L to "первый", 10L to "второй"), 20L)
        assertEquals(5L to "первый", cursor.next())
        assertEquals(5L to "второй", cursor.next())
        repeat(5) {
            assertEquals(15L to "первый", cursor.next())
            assertEquals(5L to "второй", cursor.next())
        }
    }
    @Test fun tickZeroSingleFrameCanLoopWithoutRecursion() {
        val cursor = AnimationCursor(listOf(0L to 1), 1L)
        assertEquals(0L to 1, cursor.next())
        repeat(100) { assertEquals(1L to 1, cursor.next()) }
    }
    @Test fun sessionsHaveIndependentProgress() {
        val frames = listOf(0L to 1, 10L to 2)
        val first = AnimationCursor(frames, null)
        val second = AnimationCursor(frames, null)
        first.next()
        first.next()
        assertEquals(0L to 1, second.next())
        assertNull(first.next())
    }
    @Test fun invalidTimelinesAreRejected() {
        assertFailsWith<IllegalArgumentException> { AnimationCursor(emptyList<Pair<Long, Int>>(), null) }
        assertFailsWith<IllegalArgumentException> { AnimationCursor(listOf(-1L to 1), null) }
        assertFailsWith<IllegalArgumentException> { AnimationCursor(listOf(10L to 1, 0L to 2), null) }
        assertFailsWith<IllegalArgumentException> { AnimationCursor(listOf(0L to 1, 0L to 2), null) }
        assertFailsWith<IllegalArgumentException> { AnimationCursor(listOf(10L to 1), 10L) }
    }
    @Test fun longTickOffsetsDoNotOverflowAtCycleBoundary() {
        val cursor = AnimationCursor(listOf((Long.MAX_VALUE - 2) to 1), Long.MAX_VALUE)
        assertEquals((Long.MAX_VALUE - 2) to 1, cursor.next())
        assertEquals(Long.MAX_VALUE to 1, cursor.next())
    }
}
