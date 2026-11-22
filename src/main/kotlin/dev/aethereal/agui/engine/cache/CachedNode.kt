package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.surface.item.click.GUIClick
import dev.aethereal.agui.text.TextContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal class CachedNode(
    private val item: CachedItem,
    val handler: (GUIClick.() -> Unit)?,
    private val visibility: ((Player) -> Boolean)?,
    val cooldownTicks: Int = 0,
    private val conditions: List<Pair<String, (Player) -> Boolean>> = emptyList(),
    private val disabledStyle: (dev.aethereal.agui.surface.item.DynamicIcon.(String) -> Unit)? = null,
    val denied: (GUIClick.(String) -> Unit)? = null
) {
    val hasConditions: Boolean get() = conditions.isNotEmpty()
    fun disabledReason(player: Player): String? =
        conditions.firstOrNull { !it.second(player) }?.first

    private fun disabled(item: ItemStack, reason: String, context: TextContext): ItemStack {
        val copy = item.clone()
        if (reason.isNotBlank()) {
            copy.editMeta { meta ->
                val line = context.render(dev.aethereal.agui.text.MenuText(reason))
                    .decorationIfAbsent(net.kyori.adventure.text.format.TextDecoration.ITALIC,
                        net.kyori.adventure.text.format.TextDecoration.State.FALSE)
                meta.lore(meta.lore().orEmpty() + line)
            }
        }
        val style = disabledStyle ?: return copy
        val icon = dev.aethereal.agui.surface.item.DynamicIcon(copy, context)
        style.invoke(icon, reason)
        return icon.apply()
    }

    fun isVisible(player: Player): Boolean = visibility?.invoke(player) ?: true

    fun applyState(stack: ItemStack?, context: TextContext): ItemStack? {
        val reason = if (stack != null && hasConditions) disabledReason(checkNotNull(context.player)) else null
        return if (stack != null && reason != null) disabled(stack, reason, context) else stack
    }

    fun render(context: TextContext, rendered: MutableMap<CachedNode, ItemStack?>, includeDisabled: Boolean = true): ItemStack? {
        if (this in rendered) return rendered[this]
        val stack = if (visibility == null || isVisible(checkNotNull(context.player))) item.render(context) else null
        val result = if (includeDisabled) applyState(stack, context) else stack
        rendered[this] = result
        return result
    }
}
