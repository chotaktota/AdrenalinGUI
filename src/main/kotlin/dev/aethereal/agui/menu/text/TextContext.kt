package dev.aethereal.agui.menu.text

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

internal class TextContext(val player: Player? = null, private val resolve: (String) -> String) {
    private val rendered = hashMapOf<String, Component>()
    private val values = hashMapOf<String, String>()

    fun render(text: MenuText): Component =
        if (!text.dynamic) text.component
        else rendered.getOrPut(text.source) {
            text.render { text.substitute { token -> values.getOrPut(token) { resolve(token) } } }
        }
}
