package dev.aethereal.agui.menu.text

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

internal object LegacyFormatting {
    val resolver: TagResolver = TagResolver.resolver("agui_legacy") { args, _ ->
        val value = args.popOr("Не задан legacy-формат").value()
        val decoration = TextDecoration.NAMES.value(value)
        if (decoration != null) Tag.styling(decoration)
        else {
            val color = if (value.startsWith('#')) TextColor.fromHexString(value)
                else NamedTextColor.NAMES.value(value)
            Tag.styling(java.util.function.Consumer { style ->
                style.color(checkNotNull(color) { "Неизвестный legacy-цвет: $value" })
                TextDecoration.entries.forEach { style.decoration(it, false) }
            })
        }
    }
    private val colors = arrayOf("black", "dark_blue", "dark_green", "dark_aqua",
        "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue",
        "green", "aqua", "red", "light_purple", "yellow", "white")
    private val decorations = mapOf('k' to "obfuscated", 'l' to "bold",
        'm' to "strikethrough", 'n' to "underlined", 'o' to "italic")

    fun normalize(source: String): String {
        if ('&' !in source && '§' !in source) return source
        val result = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            val current = source[index]
            if (current == '\\' && index + 1 < source.length) {
                result.append(current).append(source[index + 1])
                index += 2
                continue
            }
            if (current == '<' && index + 1 < source.length &&
                (source[index + 1].isLetter() || source[index + 1] in "/!#")) {
                val end = tagEnd(source, index + 1)
                if (end >= 0) {
                    result.append(source, index, end + 1)
                    index = end + 1
                    continue
                }
            }
            if ((current == '&' || current == '§') && index + 1 < source.length) {
                val code = source[index + 1].lowercaseChar()
                if (code == '#' && index + 8 <= source.length &&
                    source.substring(index + 2, index + 8).all(::isHex)) {
                    result.append("<agui_legacy:#").append(source, index + 2, index + 8).append('>')
                    index += 8
                    continue
                }
                if (code == 'x' && index + 14 <= source.length &&
                    (0 until 6).all { source[index + 2 + it * 2] == current && isHex(source[index + 3 + it * 2]) }) {
                    result.append("<agui_legacy:#")
                    repeat(6) { result.append(source[index + 3 + it * 2]) }
                    result.append('>')
                    index += 14
                    continue
                }
                val color = code.digitToIntOrNull(16)
                if (color != null) {
                    result.append("<agui_legacy:").append(colors[color]).append('>')
                    index += 2
                    continue
                }
                val decoration = decorations[code]
                if (decoration != null || code == 'r') {
                    if (decoration != null) result.append("<agui_legacy:").append(decoration).append('>')
                    else result.append("<agui_legacy:white>")
                    index += 2
                    continue
                }
            }
            result.append(current)
            index++
        }
        return result.toString()
    }

    private fun isHex(value: Char) = value in '0'..'9' || value in 'a'..'f' || value in 'A'..'F'

    private fun tagEnd(source: String, start: Int): Int {
        var quote: Char? = null
        var index = start
        while (index < source.length) {
            val value = source[index]
            if (value == '\\') { index += 2; continue }
            if (quote != null) {
                if (value == quote) quote = null
            } else when (value) {
                '\'', '"' -> quote = value
                '>' -> return index
            }
            index++
        }
        return -1
    }
}
