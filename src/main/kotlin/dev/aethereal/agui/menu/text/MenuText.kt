package dev.aethereal.agui.menu.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

internal class MenuText(val source: String) {
    val dynamic = placeholders.containsMatchIn(source)
    val component: Component = miniMessage.deserialize(LegacyFormatting.normalize(source), LegacyFormatting.resolver)

    fun render(resolve: (String) -> String): Component {
        if (!dynamic) return component
        val result = resolve(source)
        return if (result == source) component else miniMessage.deserialize(LegacyFormatting.normalize(result), LegacyFormatting.resolver)
    }

    private companion object {
        val miniMessage = MiniMessage.miniMessage()
        val placeholders = Regex("%[^%\\r\\n]+%")
    }
}
