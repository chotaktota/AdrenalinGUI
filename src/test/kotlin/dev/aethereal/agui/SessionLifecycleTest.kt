package dev.aethereal.agui

import dev.aethereal.agui.menu.session.SessionLifecycle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionLifecycleTest {
    @Test fun callbacksRunOnceInOrder() {
        val calls = mutableListOf<String>()
        val lifecycle = SessionLifecycle({ calls += "open" }, { calls += "close" })
        lifecycle.open()
        lifecycle.open()
        lifecycle.close()
        lifecycle.close()
        lifecycle.open()
        assertEquals(listOf("open", "close"), calls)
    }

    @Test fun cancelledOpenDoesNotNotify() {
        val calls = mutableListOf<String>()
        val lifecycle = SessionLifecycle({ calls += "open" }, { calls += "close" })
        lifecycle.close()
        lifecycle.open()
        assertEquals(emptyList(), calls)
    }

    @Test fun closingInsideOnOpenNotifiesExactlyOnce() {
        val calls = mutableListOf<String>()
        lateinit var lifecycle: SessionLifecycle
        lifecycle = SessionLifecycle({ calls += "open"; lifecycle.close() }, { calls += "close" })
        lifecycle.open()
        lifecycle.close()
        assertEquals(listOf("open", "close"), calls)
    }

    @Test fun closeCallbackCannotReenterLifecycle() {
        var closes = 0
        lateinit var lifecycle: SessionLifecycle
        lifecycle = SessionLifecycle({}, { closes++; lifecycle.close(); lifecycle.open() })
        lifecycle.open()
        lifecycle.close()
        assertEquals(1, closes)
    }

    @Test fun callbackFailureDoesNotCauseDuplicateNotification() {
        var opens = 0
        var closes = 0
        val lifecycle = SessionLifecycle({ opens++; error("open failed") }, { closes++; error("close failed") })
        assertFailsWith<IllegalStateException> { lifecycle.open() }
        lifecycle.open()
        assertFailsWith<IllegalStateException> { lifecycle.close() }
        lifecycle.close()
        assertEquals(1, opens)
        assertEquals(1, closes)
    }
}
