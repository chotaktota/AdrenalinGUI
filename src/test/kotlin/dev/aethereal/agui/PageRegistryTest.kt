package dev.aethereal.agui

import dev.aethereal.agui.menu.page.registry.PageRegistry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class PageRegistryTest {
    @Test fun pagesBuildLazilyAndReuseTheirOwnCache() {
        val pages = PageRegistry<Any>()
        var mainBuilds = 0
        var economyBuilds = 0
        pages.register("main") { mainBuilds++; Any() }
        pages.register("economy") { economyBuilds++; Any() }
        assertEquals(0, mainBuilds)
        assertEquals(0, economyBuilds)
        val main = pages.get("main")
        repeat(1000) { assertSame(main, pages.get("main")) }
        assertEquals(1, mainBuilds)
        assertEquals(0, economyBuilds)
        val economy = pages.get("economy")
        assertNotSame(main, economy)
        assertSame(economy, pages.get("economy"))
        assertEquals(1, economyBuilds)
    }

    @Test fun invalidatingOnePagePreservesOtherPages() {
        val pages = PageRegistry<Any>()
        pages.register("main") { Any() }
        pages.register("economy") { Any() }
        val main = pages.get("main")
        val economy = pages.get("economy")
        pages.invalidate("economy")
        assertSame(main, pages.get("main"))
        assertNotSame(economy, pages.get("economy"))
    }

    @Test fun unknownAndDuplicatePagesAreRejected() {
        val pages = PageRegistry<Any>()
        pages.register("main") { Any() }
        assertFailsWith<IllegalArgumentException> { pages.register("main") { Any() } }
        assertFailsWith<IllegalArgumentException> { pages.register(" ") { Any() } }
        assertFailsWith<IllegalArgumentException> { pages.get("missing") }
        assertFailsWith<IllegalArgumentException> { pages.invalidate("missing") }
    }

    @Test fun failedPageCanBeRebuiltWithoutInvalidatingOtherPages() {
        val pages = PageRegistry<Any>()
        var attempts = 0
        pages.register("main") { Any() }
        pages.register("economy") {
            attempts++
            check(attempts > 1)
            Any()
        }
        val main = pages.get("main")
        assertFailsWith<IllegalStateException> { pages.get("economy") }
        val economy = pages.get("economy")
        assertSame(economy, pages.get("economy"))
        assertSame(main, pages.get("main"))
        assertEquals(2, attempts)
    }
}
