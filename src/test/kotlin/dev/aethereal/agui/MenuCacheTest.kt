package dev.aethereal.agui

import dev.aethereal.agui.menu.cache.MenuCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class MenuCacheTest {
    @Test fun repeatedOpensBuildOnlyOnce() {
        val cache = MenuCache<Any>()
        var builds = 0
        val first = cache.getOrBuild { builds++; Any() }
        repeat(1000) { assertSame(first, cache.getOrBuild { builds++; Any() }) }
        assertEquals(1, builds)
    }

    @Test fun invalidationRebuildsAndDoesNotChangePreviousSnapshot() {
        val cache = MenuCache<Any>()
        val first = cache.getOrBuild { Any() }
        cache.clear()
        val second = cache.getOrBuild { Any() }
        assertNotSame(first, second)
        assertSame(second, cache.getOrBuild { error("Must reuse snapshot") })
    }

    @Test fun failedConstructionCanBeRetried() {
        val cache = MenuCache<Any>()
        assertFailsWith<IllegalStateException> { cache.getOrBuild { error("Failed layout") } }
        val result = cache.getOrBuild { Any() }
        assertSame(result, cache.getOrBuild { error("Must reuse snapshot") })
    }

    @Test fun instancesHaveIndependentCaches() {
        val first = MenuCache<Any>()
        val second = MenuCache<Any>()
        assertNotSame(first.getOrBuild { Any() }, second.getOrBuild { Any() })
    }
}
