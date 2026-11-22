package dev.aethereal.agui.surface.item.click.action

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.engine.session.MenuSession
import dev.aethereal.agui.text.MenuText
import org.bukkit.entity.Player

open class OtherAction internal constructor(
    player: Player,
    private val navigate: ((String) -> Unit)?,
    private val session: MenuSession
) : PlayerAction(session.engine.plugin, player) {
    fun inputItem(slot: Int): org.bukkit.inventory.ItemStack? = session.inputItem(slot)
    fun takeInput(slot: Int): org.bukkit.inventory.ItemStack? = session.takeInput(slot)

    fun hasPreviousMenu(): Boolean {
        MenuEngine.requireSync()
        return session.engine.currentSession(player) === session && session.engine.hasPreviousMenu(player)
    }

    fun back(): Boolean {
        MenuEngine.requireSync()
        return session.engine.currentSession(player) === session && session.engine.back(player)
    }

    inline fun <reified T : Any> inputChat(
        prompt: String,
        timeoutTicks: Long = 1200,
        noinline configure: Chat<T>.() -> Unit
    ) = inputChat(prompt, T::class.java, timeoutTicks, configure)

    fun <T : Any> inputChat(
        prompt: String,
        type: Class<T>,
        timeoutTicks: Long = 1200,
        configure: Chat<T>.() -> Unit
    ) {
        MenuEngine.requireSync()
        require(timeoutTicks in 1..Int.MAX_VALUE.toLong()) { "Время ожидания должно быть положительным и помещаться в Int" }
        require(prompt.isNotBlank()) { "Приглашение для ввода не может быть пустым" }
        val parser = chatParser(type)
        if (session.engine.currentSession(player) !== session) return
        val input = Chat<T>().apply(configure)
        val engine = session.engine
        val message = engine.text.forPlayer(player).render(MenuText(prompt))
        engine.chatInputs.start(player, session.owner, timeoutTicks, response = { reply ->
            val text = reply.trim()
            if (text.equals("отмена", true) || text.equals("cancel", true)) {
                input.failure.invoke(player, InputFailure.CANCELLED)
            } else {
                val value = parser(text)
                if (value == null || !input.validator(value)) {
                    input.failure.invoke(player, InputFailure.INVALID_INPUT)
                } else {
                    input.success.invoke(player, value)
                }
            }
        }, expired = { input.failure.invoke(player, InputFailure.TIMEOUT) })
        try {
            session.close()
            if (engine.chatInputs.waiting(player)) player.sendMessage(message)
        } catch (exception: Exception) {
            engine.chatInputs.clear(player)
            throw exception
        }
    }

    fun refresh(vararg slots: Int) {
        MenuEngine.requireSync()
        val session = player.openInventory.topInventory.holder as? MenuSession ?: return
        session.refresh(if (slots.isEmpty()) IntArray(session.inventory.size) { it } else slots)
    }
    fun close() { MenuEngine.requireSync(); player.closeInventory() }
    fun openPage(page: String) {
        MenuEngine.requireSync()
        checkNotNull(navigate) { "Переход между страницами доступен только в MultiGUISurface" }.invoke(page)
    }

    enum class InputFailure {
        INVALID_INPUT,
        CANCELLED,
        TIMEOUT
    }

    class Chat<T : Any> internal constructor() {
        internal var validator: (T) -> Boolean = { true }
            private set
        internal var success: Player.(T) -> Unit = {}
            private set
        internal var failure: Player.(InputFailure) -> Unit = {
            sendMessage(when (it) {
                InputFailure.INVALID_INPUT -> "Некорректный ввод"
                InputFailure.CANCELLED -> "Ввод отменён"
                InputFailure.TIMEOUT -> "Время ожидания ввода истекло"
            })
        }
            private set

        fun validate(condition: (T) -> Boolean) {
            validator = condition
        }

        fun onSuccess(action: Player.(T) -> Unit) {
            success = action
        }

        fun onFailure(action: Player.(InputFailure) -> Unit) {
            failure = action
        }
    }

}

private fun <T : Any> chatParser(type: Class<T>): (String) -> T? {
    val boxed = type.kotlin.javaObjectType
    val parse: (String) -> Any? = when (type.kotlin) {
        String::class -> { input -> input.takeIf { it.isNotBlank() } }
        Int::class -> String::toIntOrNull
        Long::class -> String::toLongOrNull
        Double::class -> { input -> input.toDoubleOrNull()?.takeIf { it.isFinite() } }
        Float::class -> { input -> input.toFloatOrNull()?.takeIf { it.isFinite() } }
        Boolean::class -> { input -> when (input.lowercase()) {
            "true", "да" -> true
            "false", "нет" -> false
            else -> null
        } }
        else -> throw IllegalArgumentException("Неподдерживаемый тип ввода: ${type.name}")
    }
    return { input -> parse(input)?.let(boxed::cast) }
}
