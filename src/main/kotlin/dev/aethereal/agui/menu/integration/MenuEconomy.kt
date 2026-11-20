package dev.aethereal.agui.menu.integration

import dev.aethereal.agui.menu.MenuEngine
import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

class MenuEconomy internal constructor(private val plugin: Plugin) {
    data class Transaction(val success: Boolean, val amount: Double, val error: String?)
    private fun provider(): Economy {
        MenuEngine.requireSync()
        check(plugin.server.pluginManager.isPluginEnabled("Vault")) { "Vault недоступен" }
        return checkNotNull(plugin.server.servicesManager.load(Economy::class.java)) { "Экономический провайдер Vault недоступен" }
    }
    fun balance(player: OfflinePlayer): Double = provider().getBalance(player)
    fun has(player: OfflinePlayer, amount: Double): Boolean { validate(amount); return provider().has(player, amount) }
    fun format(amount: Double): String { validate(amount); return provider().format(amount) }
    fun withdraw(player: OfflinePlayer, amount: Double): Transaction {
        validate(amount)
        val response = provider().withdrawPlayer(player, amount)
        return Transaction(response.transactionSuccess(), response.amount, response.errorMessage)
    }
    fun deposit(player: OfflinePlayer, amount: Double): Transaction {
        validate(amount)
        val response = provider().depositPlayer(player, amount)
        return Transaction(response.transactionSuccess(), response.amount, response.errorMessage)
    }
    private fun validate(amount: Double) { require(amount.isFinite() && amount >= 0) { "Сумма должна быть конечным неотрицательным числом" } }
}
