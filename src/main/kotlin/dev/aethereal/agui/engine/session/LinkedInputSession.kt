package dev.aethereal.agui.engine.session

import dev.aethereal.agui.engine.cache.CachedInput
import dev.aethereal.agui.surface.item.InputTransfer
import dev.aethereal.agui.text.TextContext
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

internal class LinkedInputSession(
    val definition: CachedInput,
    private val inventory: Inventory,
    private val accepts: (ItemStack) -> Boolean
) {
    private var original: ItemStack? = null

    fun item(): ItemStack? = original?.clone()

    fun render(context: TextContext) {
        val item = original
        val placeholder = if (item == null) definition.placeholder(context) else null
        val display = item?.let { definition.display(it, context) }
        inventory.setItem(definition.source, placeholder)
        inventory.setItem(definition.target, display)
    }

    fun take(context: TextContext): ItemStack? {
        val item = original ?: return null
        val placeholder = definition.placeholder(context)
        original = null
        inventory.setItem(definition.target, null)
        inventory.setItem(definition.source, placeholder)
        return item.clone()
    }

    fun drain(): ItemStack? {
        val item = original
        original = null
        inventory.setItem(definition.target, null)
        inventory.setItem(definition.source, null)
        return item
    }

    fun click(slot: Int, click: ClickType, player: Player, context: TextContext) {
        if (slot == definition.source) {
            insert(click, player, context)
        } else {
            withdraw(click, player, context)
        }
    }

    private fun insert(click: ClickType, player: Player, context: TextContext) {
        if (original != null || click != ClickType.LEFT && click != ClickType.RIGHT) return
        val cursor = player.itemOnCursor
        if (cursor.isEmpty) return
        val amount = when (definition.transfer) {
            InputTransfer.ALL -> cursor.amount
            InputTransfer.ONE -> 1
            InputTransfer.CLICK -> if (click == ClickType.RIGHT) 1 else cursor.amount
        }
        val item = cursor.clone().apply { this.amount = amount }
        if (!accepts(item)) return
        val display = definition.display(item, context)
        val remainder = if (cursor.amount == amount) null else cursor.clone().apply { this.amount -= amount }
        original = item
        player.setItemOnCursor(remainder)
        inventory.setItem(definition.source, null)
        inventory.setItem(definition.target, display)
    }

    private fun withdraw(click: ClickType, player: Player, context: TextContext) {
        val item = original ?: return
        val placeholder = definition.placeholder(context)
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            val leftovers = player.inventory.addItem(item.clone())
            original = leftovers.values.firstOrNull()
            render(context)
            return
        }
        if (click != ClickType.LEFT && click != ClickType.RIGHT) return
        val cursor = player.itemOnCursor
        if (!cursor.isEmpty && !cursor.isSimilar(item)) return
        val available = if (cursor.isEmpty) item.maxStackSize else (cursor.maxStackSize - cursor.amount).coerceAtLeast(0)
        val requested = if (click == ClickType.RIGHT) (item.amount + 1) / 2 else item.amount
        val amount = minOf(available, requested)
        if (amount == 0) return
        val next = item.clone().apply { this.amount = amount + if (cursor.isEmpty) 0 else cursor.amount }
        val remainder = if (amount == item.amount) null else item.clone().apply { this.amount -= amount }
        val display = remainder?.let { definition.display(it, context) }
        original = remainder
        player.setItemOnCursor(next)
        inventory.setItem(definition.target, display)
        inventory.setItem(definition.source, if (remainder == null) placeholder else null)
    }
}
