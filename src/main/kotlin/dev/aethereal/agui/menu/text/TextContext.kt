package dev.aethereal.agui.menu.text

import net.kyori.adventure.text.Component

internal class TextContext(private val resolve: (String) -> String) {
    private val rendered = hashMapOf<String, Component>()

    fun render(text: MenuText): Component =
        if (!text.dynamic) text.component
        else rendered.getOrPut(text.source) { text.render(resolve) }
}
