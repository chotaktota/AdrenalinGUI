package dev.aethereal.agui.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

internal class MenuText(val source: String) {
    private val tokens = placeholders.findAll(source).map { it.value }.toList()
    private val fragments = if (tokens.isEmpty()) emptyList() else placeholders.split(source)
    val dynamic = tokens.isNotEmpty()

    fun substitute(resolve: (String) -> String): String = buildString(source.length) {
        append(fragments[0])
        tokens.forEachIndexed { index, token -> append(resolve(token)); append(fragments[index + 1]) }
    }
    val component: Component = miniMessage.deserialize(LegacyFormatting.normalize(source), LegacyFormatting.resolver)

    private val results = object : LinkedHashMap<String, Component>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Component>?) = size > 16
    }

    fun render(resolve: (String) -> String): Component {
        if (!dynamic) return component
        val result = resolve(source)
        return if (result == source) component else results.getOrPut(result) {
            miniMessage.deserialize(LegacyFormatting.normalize(result), LegacyFormatting.resolver)
        }
    }

    private companion object {
        val miniMessage = MiniMessage.miniMessage()
        val placeholders = Regex("%[^%\\r\\n]+%")
    }
}
