package dev.aethereal.agui.surface.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.engine.session.MenuSession
import dev.aethereal.agui.text.MenuText

class GUIClick internal constructor(
    val player: Player,
    val slot: Int,
    val click: ClickType,
    val page: String? = null,
    private val navigate: ((String) -> Unit)?,
    private val session: MenuSession
) {
    val isLeftClick: Boolean get() = click.isLeftClick
    val isRightClick: Boolean get() = click.isRightClick
    val isShiftClick: Boolean get() = click.isShiftClick
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
        noinline configure: ChatInput<T>.() -> Unit
    ) = inputChat(prompt, T::class.java, timeoutTicks, configure)

    fun <T : Any> inputChat(
        prompt: String,
        type: Class<T>,
        timeoutTicks: Long = 1200,
        configure: ChatInput<T>.() -> Unit
    ) {
        MenuEngine.requireSync()
        require(timeoutTicks in 1..Int.MAX_VALUE.toLong()) { "Время ожидания должно быть положительным и помещаться в Int" }
        require(prompt.isNotBlank()) { "Приглашение для ввода не может быть пустым" }
        val parser = chatParser(type)
        if (session.engine.currentSession(player) !== session) return
        val input = ChatInput<T>().apply(configure)
        val engine = session.engine
        val message = engine.text.forPlayer(player).render(MenuText(prompt))
        engine.chatInputs.start(player, session.owner, timeoutTicks, response = { reply ->
            val text = reply.trim()
            if (text.equals("отмена", true) || text.equals("cancel", true)) {
                input.failure.invoke(player, ChatInputFailure.CANCELLED)
            } else {
                val value = parser(text)
                if (value == null || !input.validator(value)) {
                    input.failure.invoke(player, ChatInputFailure.INVALID_INPUT)
                } else {
                    input.success.invoke(player, value)
                }
            }
        }, expired = { input.failure.invoke(player, ChatInputFailure.TIMEOUT) })
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
        val session = player.openInventory.topInventory.holder as? dev.aethereal.agui.engine.session.MenuSession ?: return
        session.refresh(if (slots.isEmpty()) IntArray(session.inventory.size) { it } else slots)
    }
    fun close() { MenuEngine.requireSync(); player.closeInventory() }
    fun openPage(page: String) {
        MenuEngine.requireSync()
        checkNotNull(navigate) { "Переход между страницами доступен только в MultiGUISurface" }.invoke(page)
    }
}
