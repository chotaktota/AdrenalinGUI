package dev.aethereal.agui.menu.session

import dev.aethereal.agui.menu.cache.CachedAnimation
import dev.aethereal.agui.menu.cache.CachedNode
import dev.aethereal.agui.menu.text.TextContext
import dev.aethereal.agui.surface.icon.DynamicIcon
import org.bukkit.inventory.Inventory

internal class AnimatedContents(private val nodes: Array<CachedNode?>) {
    private val visible = BooleanArray(nodes.size) { true }
    private val versions = LongArray(nodes.size)
    private val changes = arrayOfNulls<LinkedHashSet<CachedAnimation.Operation.Change>>(nodes.size)
    fun revision(slot: Int) = versions[slot]
    fun handler(slot: Int) = if (visible[slot]) nodes[slot]?.handler else null
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
        val rendered = java.util.IdentityHashMap<dev.aethereal.agui.menu.cache.CachedItem, org.bukkit.inventory.ItemStack>()
        for (slot in slots.distinct()) {
            val node = nodes[slot]
            val item = if (!visible[slot] || node == null) null else {
                val copy = rendered.getOrPut(node.item) { node.item.render(context) }.clone()
                changes[slot]?.let { edits ->
                    val icon = DynamicIcon(copy, context)
                    edits.forEach { it.configure.invoke(icon, checkNotNull(context.player)) }
                    icon.apply()
                }
                copy
            }
            if (inventory.getItem(slot) != item) inventory.setItem(slot, item)
        }
    }
}
