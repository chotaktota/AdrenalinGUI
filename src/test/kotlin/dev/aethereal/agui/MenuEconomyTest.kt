package dev.aethereal.agui

import dev.aethereal.agui.integration.MenuEconomy
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.ServicesManager
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MenuEconomyTest {
    private inline fun <reified T> proxy(crossinline call: (String, Array<out Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            call(method.name, args ?: emptyArray())
        } as T

    @Test fun transactionsValidateAmountsAndRespectProviderResults() {
        var balance = 100.0
        var available = true
        var sync = true
        val economy = proxy<Economy> { method, args -> when (method) {
            "getBalance" -> balance
            "has" -> balance >= args.last() as Double
            "format" -> args[0].toString()
            "withdrawPlayer", "depositPlayer" -> {
                val amount = args.last() as Double
                val success = method == "depositPlayer" || balance >= amount
                if (success) balance += if (method == "depositPlayer") amount else -amount
                EconomyResponse(if (success) amount else 0.0, balance,
                    if (success) EconomyResponse.ResponseType.SUCCESS else EconomyResponse.ResponseType.FAILURE,
                    if (success) "" else "Недостаточно средств")
            }
            else -> null
        } }
        val manager = proxy<PluginManager> { method, _ -> if (method == "isPluginEnabled") true else null }
        val services = proxy<ServicesManager> { method, _ -> if (method == "load" && available) economy else null }
        val server = proxy<Server> { method, _ -> when (method) {
            "isPrimaryThread" -> sync
            "getPluginManager" -> manager
            "getServicesManager" -> services
            "getLogger" -> Logger.getLogger("MenuEconomyTest")
            "getName", "getVersion", "getBukkitVersion" -> "test"
            else -> null
        } }
        val field = Bukkit::class.java.getDeclaredField("server").apply { isAccessible = true }
        val previous = field.get(null)
        field.set(null, server)
        try {
            val plugin = proxy<Plugin> { method, _ -> if (method == "getServer") server else null }
            val player = proxy<OfflinePlayer> { method, _ -> if (method == "getUniqueId") UUID(0, 1) else null }
            val access = MenuEconomy(plugin)
            assertEquals(100.0, access.balance(player))
            assertTrue(access.withdraw(player, 40.0).success)
            assertEquals(60.0, access.balance(player))
            assertFalse(access.withdraw(player, 100.0).success)
            assertEquals(60.0, access.balance(player))
            assertTrue(access.deposit(player, 10.0).success)
            assertEquals(70.0, access.balance(player))
            for (amount in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
                assertFailsWith<IllegalArgumentException> { access.withdraw(player, amount) }
            }
            available = false
            assertFailsWith<IllegalStateException> { access.balance(player) }
            available = true
            sync = false
            assertFailsWith<IllegalStateException> { access.balance(player) }
        } finally { field.set(null, previous) }
    }
}
