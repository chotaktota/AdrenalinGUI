package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.surface.item.InputField
import dev.aethereal.agui.surface.item.InputTransfer
import dev.aethereal.agui.text.TextContext
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack

internal class CachedInput(val source: Int, val target: Int, field: InputField) {
    val transfer: InputTransfer = field.transferMode
    private val placeholder = field.icon.compile()
    private val lines = field.lines.toList()

    fun placeholder(context: TextContext): ItemStack = placeholder.render(context).clone()

    fun display(original: ItemStack, context: TextContext): ItemStack = original.clone().apply {
        if (lines.isNotEmpty()) {
            val meta = checkNotNull(itemMeta) { "Предмет не поддерживает ItemMeta" }
            meta.lore(meta.lore().orEmpty() + lines.map {
                context.render(it).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            })
            check(setItemMeta(meta)) { "Не удалось применить лор поля" }
        }
    }
}
