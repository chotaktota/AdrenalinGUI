package dev.aethereal.agui

import dev.aethereal.agui.command.MenuListPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MenuListPageTest {
    @Test fun emptyCatalogStillHasOnePage() {
        val page = MenuListPage(emptyList<Int>(), 99)
        assertEquals(1, page.count)
        assertEquals(0, page.index)
        assertEquals(emptyList(), page.entries)
    }
    @Test fun exactBoundaryDoesNotCreateExtraPage() {
        val page = MenuListPage((0 until 45).toList(), 1)
        assertEquals(1, page.count)
        assertEquals(0, page.index)
        assertEquals(45, page.entries.size)
    }
    @Test fun pagesNeitherDropNorDuplicateEntries() {
        val values = (0 until 91).toList()
        val pages = (0..2).map { MenuListPage(values, it) }
        assertEquals(listOf(45, 45, 1), pages.map { it.entries.size })
        assertEquals(values, pages.flatMap { it.entries })
    }
    @Test fun stalePageClampsToTheRemainingEntries() {
        assertEquals(1, MenuListPage((0 until 46).toList(), Int.MAX_VALUE).index)
        assertEquals(0, MenuListPage(listOf(1), -1).index)
        assertFailsWith<IllegalArgumentException> { MenuListPage(listOf(1), 0, 0) }
    }
}
