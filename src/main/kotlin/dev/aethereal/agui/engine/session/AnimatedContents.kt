package dev.aethereal.agui.engine.session

import dev.aethereal.agui.engine.cache.CachedAnimation
import dev.aethereal.agui.engine.cache.CachedNode
import dev.aethereal.agui.text.TextContext
import dev.aethereal.agui.surface.item.DynamicIcon
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.IdentityHashMap

internal class AnimatedContents(private val nodes: Array<CachedNode?>) {
    private val visible = BooleanArray(nodes.size) { true }
    private val versions = LongArray(nodes.size)
    private val changes = arrayOfNulls<LinkedHashSet<CachedAnimation.Operation.Change>>(nodes.size)
    fun revision(slot: Int) = versions[slot]
    fun nodeAt(slot: Int): CachedNode? = if (visible.getOrNull(slot) == true) nodes[slot] else null
    fun apply(operations: List<CachedAnimation.Operation>): IntArray {
        val dirty = linkedSetOf<Int>()
        for (operation in operations) for (slot in operation.slots) {
            when (operation) {
                is CachedAnimation.Operation.Set -> { nodes[slot] = operation.node; visible[slot] = true; changes[slot] = null }
                is CachedAnimation.Operation.Visibility -> visible[slot] = operation.visible
                is CachedAnimation.Operation.Change -> if (nodes[slot] != null) {
                    val edits = changes[slot] ?: linkedSetOf<CachedAnimation.Operation.Change>().also { changes[slot] = it }
                    edits.remove(operation)
                    edits.add(operation)
                }
            }
            versions[slot]++
            dirty.add(slot)
        }
        return dirty.toIntArray()
    }
    fun refresh(inventory: Inventory, context: TextContext, slots: IntArray) {
        require(slots.all { it in nodes.indices }) { "Слот за пределами меню" }
        val rendered = IdentityHashMap<CachedNode, ItemStack?>()
        for (slot in slots.distinct()) {
            val item = nodeAt(slot)?.render(context, rendered, includeDisabled = false)?.let { renderedItem ->
                var copy = renderedItem.clone()
                changes[slot]?.let { edits ->
                    val icon = DynamicIcon(copy, context)
                    edits.forEach { it.configure.invoke(icon, checkNotNull(context.player)) }
                    copy = icon.apply()
                }
                nodeAt(slot)?.applyState(copy, context)
            }
            if (inventory.getItem(slot) != item) inventory.setItem(slot, item)
        }
    }
}
