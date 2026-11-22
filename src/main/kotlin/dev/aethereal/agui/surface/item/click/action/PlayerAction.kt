package dev.aethereal.agui.surface.item.click.action

import dev.aethereal.agui.engine.MenuEngine
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.InheritanceNode
import net.luckperms.api.node.types.PermissionNode
import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

open class PlayerAction(private val plugin: Plugin, player: Player? = null) : ItemAction(player) {
    data class Transaction(val success: Boolean, val amount: Double, val error: String?)
    private fun provider(): Economy {
        MenuEngine.requireSync()
        check(plugin.server.pluginManager.isPluginEnabled("Vault")) { "Vault недоступен" }
        return checkNotNull(plugin.server.servicesManager.load(Economy::class.java)) { "Экономический провайдер Vault недоступен" }
    }
    fun balance(player: OfflinePlayer = this.player): Double = provider().getBalance(player)
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

    val isAvailable: Boolean
        get() {
            MenuEngine.requireSync()
            return plugin.server.pluginManager.isPluginEnabled("LuckPerms") &&
                plugin.server.servicesManager.load(LuckPerms::class.java) != null
        }

    val api: LuckPerms
        get() {
            check(isAvailable) { "LuckPerms не установлен или его API недоступен" }
            return checkNotNull(plugin.server.servicesManager.load(LuckPerms::class.java)) { "API LuckPerms недоступен" }
        }

    fun user(uuid: UUID = player.uniqueId): User? = api.userManager.getUser(uuid)

    fun loadUser(uuid: UUID = player.uniqueId): CompletableFuture<User> = api.userManager.loadUser(uuid)

    fun cleanupUser(user: User) = api.userManager.cleanupUser(user)

    fun modifyUser(uuid: UUID, action: (User) -> Unit): CompletableFuture<Void> =
        api.userManager.modifyUser(uuid, action)

    fun primaryGroup(uuid: UUID = player.uniqueId): String? = user(uuid)?.primaryGroup

    fun hasPermission(player: Player, permission: String): Boolean? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)?.cachedData
            ?.getPermissionData(api.contextManager.getQueryOptions(player))
            ?.checkPermission(permission)?.asBoolean()
    }

    fun prefix(player: Player = this.player): String? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)?.cachedData
            ?.getMetaData(api.contextManager.getQueryOptions(player))?.prefix
    }

    fun suffix(player: Player = this.player): String? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)?.cachedData
            ?.getMetaData(api.contextManager.getQueryOptions(player))?.suffix
    }

    fun groups(player: Player = this.player): Set<String>? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)
            ?.getInheritedGroups(api.contextManager.getQueryOptions(player))?.map { it.name }?.toSet()
    }

    fun setPermission(uuid: UUID, permission: String, value: Boolean = true, duration: Duration? = null): CompletableFuture<Void> {
        require(permission.isNotBlank()) { "Право не должно быть пустым" }
        require(duration == null || (!duration.isNegative && !duration.isZero)) { "Срок действия должен быть положительным" }
        val node = api.nodeBuilderRegistry.forPermission().permission(permission).value(value)
            .apply { if (duration != null) expiry(duration) }.build()
        return modifyUser(uuid) { user ->
            user.data().clear { it is PermissionNode && it.permission.equals(permission, true) && it.contexts.isEmpty }
            user.data().add(node)
        }
    }

    fun unsetPermission(uuid: UUID, permission: String): CompletableFuture<Void> {
        require(permission.isNotBlank()) { "Право не должно быть пустым" }
        return modifyUser(uuid) { user ->
            user.data().clear { it is PermissionNode && it.permission.equals(permission, true) && it.contexts.isEmpty }
        }
    }

    fun addGroup(uuid: UUID, group: String, duration: Duration? = null): CompletableFuture<Void> {
        require(group.isNotBlank()) { "Группа не должна быть пустой" }
        require(duration == null || (!duration.isNegative && !duration.isZero)) { "Срок действия должен быть положительным" }
        val node = api.nodeBuilderRegistry.forInheritance().group(group)
            .apply { if (duration != null) expiry(duration) }.build()
        return modifyUser(uuid) { user ->
            user.data().clear { it is InheritanceNode && it.groupName == node.groupName && it.contexts.isEmpty }
            user.data().add(node)
        }
    }

    fun removeGroup(uuid: UUID, group: String): CompletableFuture<Void> {
        require(group.isNotBlank()) { "Группа не должна быть пустой" }
        return modifyUser(uuid) { user ->
            user.data().clear { it is InheritanceNode && it.groupName.equals(group, true) && it.contexts.isEmpty }
        }
    }

    fun hasMoney(amount: Double): Boolean = has(player, amount)
    fun withdraw(amount: Double): Transaction = withdraw(player, amount)
    fun deposit(amount: Double): Transaction = deposit(player, amount)

    fun hasPermission(permission: String): Boolean {
        MenuEngine.requireSync()
        return player.hasPermission(permission)
    }

    fun setPermission(permission: String, value: Boolean = true, duration: Duration? = null) =
        setPermission(player.uniqueId, permission, value, duration)

    fun unsetPermission(permission: String) = unsetPermission(player.uniqueId, permission)
    fun addGroup(group: String, duration: Duration? = null) = addGroup(player.uniqueId, group, duration)
    fun removeGroup(group: String) = removeGroup(player.uniqueId, group)
}
