package dev.aethereal.agui

import dev.aethereal.agui.surface.layout.Pattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PatternTest {
    @Test fun repeatedSymbolsResolveAllSlots() {
        val pattern = Pattern("A.......A\n....B....", 2)
        assertEquals(listOf(0, 8), pattern.pos('A'))
        assertEquals(listOf(13), pattern.pos('B'))
        assertFailsWith<IllegalArgumentException> { pattern.pos('.') }
        assertFailsWith<IllegalArgumentException> { pattern.pos('Z') }
    }
    @Test fun malformedLayoutsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Pattern("........", 1) }
        assertFailsWith<IllegalArgumentException> { Pattern("..........", 1) }
        assertFailsWith<IllegalArgumentException> { Pattern(".........", 2) }
    }
    @Test fun indentationDoesNotChangeSlots() {
        val pattern = Pattern("\n    ....A....\n    B........\n", 2)
        assertEquals(listOf(4), pattern.pos('A'))
        assertEquals(listOf(9), pattern.pos('B'))
    }
}
