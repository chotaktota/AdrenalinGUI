package dev.aethereal.agui.surface.item

import dev.aethereal.agui.surface.item.builder.ItemBuilder
import dev.aethereal.agui.surface.item.click.GUIClick
import dev.aethereal.agui.engine.MenuEngine
import org.bukkit.entity.Player

class Icon : ItemBuilder() {
    internal var update: (DynamicIcon.(Player) -> Unit)? = null
    fun dynamic(action: DynamicIcon.(Player) -> Unit) { MenuEngine.requireSync(); update = action }
    internal var visibility: ((Player) -> Boolean)? = null
        private set

    fun visibleIf(condition: (Player) -> Boolean) {
        MenuEngine.requireSync()
        val previous = visibility
        visibility = if (previous == null) condition else { player -> previous(player) && condition(player) }
    }

    fun requirePermission(permission: String) {
        require(permission.isNotBlank()) { "Право не может быть пустым" }
        visibleIf { it.hasPermission(permission) }
    }

    internal var cooldownTicks = 0
        private set
    fun cooldown(ticks: Int) {
        MenuEngine.requireSync()
        require(ticks >= 0) { "Задержка не может быть отрицательной" }
        cooldownTicks = ticks
    }

    internal var handler: (GUIClick.() -> Unit)? = null
    fun back() = onClick { back() }

    fun onClick(action: GUIClick.() -> Unit) { MenuEngine.requireSync(); handler = action }
}
