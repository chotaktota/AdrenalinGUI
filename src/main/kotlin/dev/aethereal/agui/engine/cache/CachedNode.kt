package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.surface.item.click.GUIClick
import dev.aethereal.agui.text.TextContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal class CachedNode(
    private val item: CachedItem,
    val handler: (GUIClick.() -> Unit)?,
    private val visibility: ((Player) -> Boolean)?,
    val cooldownTicks: Int = 0
) {
    fun isVisible(player: Player): Boolean = visibility?.invoke(player) ?: true

    fun render(context: TextContext, rendered: MutableMap<CachedNode, ItemStack?>): ItemStack? {
        if (this in rendered) return rendered[this]
        val stack = if (visibility == null || isVisible(checkNotNull(context.player))) item.render(context) else null
        rendered[this] = stack
        return stack
    }
}
