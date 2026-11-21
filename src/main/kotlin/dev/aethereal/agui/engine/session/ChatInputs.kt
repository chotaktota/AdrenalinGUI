package dev.aethereal.agui.engine.session

import dev.aethereal.agui.engine.MenuScheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.plugin.IllegalPluginAccessException
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

internal class ChatInputs(
    private val tasks: MenuScheduler,
    private val schedule: (() -> Unit) -> Unit
) {
    private class Key(private val player: Player) {
        override fun hashCode() = System.identityHashCode(player)
        override fun equals(other: Any?) = other is Key && other.player === player
    }

    private class Pending(
        val owner: Plugin,
        val response: (String) -> Unit,
        val expired: () -> Unit
    ) {
        val claimed = AtomicBoolean()
        var timer: MenuScheduler.Handle? = null
    }

    private val pending = ConcurrentHashMap<Key, Pending>()

    fun start(
        player: Player,
        owner: Plugin,
        timeout: Long,
        response: (String) -> Unit,
        expired: () -> Unit
    ) {
        clear(player)
        val key = Key(player)
        val input = Pending(owner, response, expired)
        pending[key] = input
        input.timer = tasks.later(timeout) {
            if (pending.remove(key, input) && owner.isEnabled && player.isOnline) {
                call(input) { expired() }
            }
        }
    }

    fun waiting(player: Player): Boolean = pending.containsKey(Key(player))

    fun capture(player: Player, message: Component): Boolean {
        val key = Key(player)
        val input = pending[key] ?: return false
        if (input.claimed.compareAndSet(false, true)) {
            try {
                schedule {
                    if (pending.remove(key, input)) {
                        input.timer?.cancel()
                        if (input.owner.isEnabled && player.isOnline) call(input) {
                            input.response(PlainTextComponentSerializer.plainText().serialize(message))
                        }
                    }
                }
            } catch (_: IllegalPluginAccessException) {
                return false
            } catch (_: RejectedExecutionException) {
                return false
            }
        }
        return true
    }

    fun clear(player: Player) {
        pending.remove(Key(player))?.timer?.cancel()
    }

    fun clear(owner: Plugin? = null) {
        pending.entries.removeIf { (_, input) ->
            if (owner == null || input.owner === owner) {
                input.timer?.cancel()
                true
            } else false
        }
    }

    private fun call(input: Pending, action: () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            input.owner.logger.log(Level.SEVERE, "Ошибка обработчика ввода в чате", exception)
        }
    }
}
