package dev.aethereal.agui.surface.item

import org.bukkit.entity.Player

enum class ChatInputFailure {
    INVALID_INPUT,
    CANCELLED,
    TIMEOUT
}

class ChatInput<T : Any> internal constructor() {
    internal var validator: (T) -> Boolean = { true }
        private set
    internal var success: Player.(T) -> Unit = {}
        private set
    internal var failure: Player.(ChatInputFailure) -> Unit = {
        sendMessage(when (it) {
            ChatInputFailure.INVALID_INPUT -> "Некорректный ввод"
            ChatInputFailure.CANCELLED -> "Ввод отменён"
            ChatInputFailure.TIMEOUT -> "Время ожидания ввода истекло"
        })
    }
        private set

    fun validate(condition: (T) -> Boolean) {
        validator = condition
    }

    fun onSuccess(action: Player.(T) -> Unit) {
        success = action
    }

    fun onFailure(action: Player.(ChatInputFailure) -> Unit) {
        failure = action
    }
}

internal fun <T : Any> chatParser(type: Class<T>): (String) -> T? {
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
