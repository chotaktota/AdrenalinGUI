package dev.aethereal.agui

import dev.aethereal.agui.engine.MenuRegistry
import dev.aethereal.agui.surface.GUISurface
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MenuRegistrySnapshotTest {
    @Test fun snapshotsDoNotFollowLaterRegistrationsAndCannotMutateRegistry() {
        val server = Proxy.newProxyInstance(Server::class.java.classLoader, arrayOf(Server::class.java)) { _, method, _ ->
            if (method.name == "isPrimaryThread") true else null
        } as Server
        val plugin = Proxy.newProxyInstance(Plugin::class.java.classLoader, arrayOf(Plugin::class.java)) { _, method, _ ->
            when (method.name) { "isEnabled" -> true; "getName" -> "Test"; else -> null }
        } as Plugin
        var invalidations = 0
        val menu = object : GUISurface {
            override val plugin = plugin
            override fun open(player: Player) {}
            override fun invalidateCache() { invalidations++ }
        }
        val field = Bukkit::class.java.getDeclaredField("server").apply { isAccessible = true }
        val previous = field.get(null)
        field.set(null, server)
        try {
            val registry = MenuRegistry()
            registry.register("first", menu)
            val snapshot = registry.entries()
            registry.register("second", menu)
            assertEquals(setOf("first"), snapshot.keys)
            val second = registry.entries() as MutableMap<String, GUISurface>
            second.clear()
            assertEquals(2, registry.entries().size)
            assertSame(menu, registry.find("first"))
            registry.unregister("first")
            assertEquals(1, invalidations)
            assertSame(menu, snapshot["first"])
        } finally { field.set(null, previous) }
    }
}
