package dev.aethereal.agui.menu.integration

import dev.aethereal.agui.menu.MenuEngine
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.InheritanceNode
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

class MenuPermissions internal constructor(private val plugin: Plugin) {
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

    fun user(uuid: UUID): User? = api.userManager.getUser(uuid)

    fun loadUser(uuid: UUID): CompletableFuture<User> = api.userManager.loadUser(uuid)

    fun cleanupUser(user: User) = api.userManager.cleanupUser(user)

    fun modifyUser(uuid: UUID, action: (User) -> Unit): CompletableFuture<Void> =
        api.userManager.modifyUser(uuid, action)

    fun primaryGroup(uuid: UUID): String? = user(uuid)?.primaryGroup

    fun hasPermission(player: Player, permission: String): Boolean? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)?.cachedData
            ?.getPermissionData(api.contextManager.getQueryOptions(player))
            ?.checkPermission(permission)?.asBoolean()
    }

    fun prefix(player: Player): String? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)?.cachedData
            ?.getMetaData(api.contextManager.getQueryOptions(player))?.prefix
    }

    fun suffix(player: Player): String? {
        MenuEngine.requireSync()
        val api = api
        return api.userManager.getUser(player.uniqueId)?.cachedData
            ?.getMetaData(api.contextManager.getQueryOptions(player))?.suffix
    }

    fun groups(player: Player): Set<String>? {
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
}
