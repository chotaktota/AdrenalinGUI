package dev.aethereal.agui.surface.item.click.action

import dev.aethereal.agui.engine.MenuEngine
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

open class ItemAction(private val targetPlayer: Player? = null) {
    val player: Player
        get() {
            MenuEngine.requireSync()
            return checkNotNull(targetPlayer) { "Для этого действия нужно указать игрока" }
        }

    fun countItem(material: Material): Int = countMatching { it.type == material }

    fun countItem(item: ItemStack): Int = countMatching { it.isSimilar(item) }

    fun hasItem(material: Material, amount: Int = 1): Boolean {
        requirePositive(amount)
        return countItem(material) >= amount
    }

    fun hasItem(item: ItemStack, amount: Int = item.amount): Boolean {
        requirePositive(amount)
        return countItem(item) >= amount
    }

    fun freeInventorySlots(): Int = inventory().storageContents.count { it.isVacant() }

    fun isInventoryFull(): Boolean = freeInventorySlots() == 0

    fun spaceForItem(item: ItemStack): Int {
        val inventory = inventory()
        require(!item.isEmpty) { "Нельзя проверить место для пустого предмета" }
        val limit = stackLimit(item, inventory)
        return inventory.storageContents.sumOf {
            when {
                it.isVacant() -> limit
                it!!.isSimilar(item) -> (limit - it.amount).coerceAtLeast(0)
                else -> 0
            }
        }
    }

    fun canFitItem(item: ItemStack, amount: Int = item.amount): Boolean {
        requirePositive(amount)
        return spaceForItem(item) >= amount
    }

    fun giveItem(material: Material, amount: Int = 1): Boolean {
        MenuEngine.requireSync()
        return giveItem(ItemStack.of(material), amount)
    }

    fun giveItem(item: ItemStack, amount: Int = item.amount): Boolean {
        val inventory = inventory()
        requirePositive(amount)
        require(!item.isEmpty) { "Нельзя выдать пустой предмет" }

        val original = inventory.storageContents
        val updated = original.copyOf()
        val limit = stackLimit(item, inventory)
        var remaining = amount
        for ((slot, current) in original.withIndex()) {
            if (current.isVacant() || !current!!.isSimilar(item)) continue
            val added = minOf(remaining, (limit - current.amount).coerceAtLeast(0))
            if (added == 0) continue
            updated[slot] = current.clone().apply { this.amount += added }
            remaining -= added
            if (remaining == 0) break
        }
        if (remaining > 0) {
            for ((slot, current) in original.withIndex()) {
                if (!current.isVacant()) continue
                val added = minOf(remaining, limit)
                updated[slot] = item.clone().apply { this.amount = added }
                remaining -= added
                if (remaining == 0) break
            }
        }
        if (remaining > 0) return false
        applyChanges(inventory, original, updated)
        return true
    }

    fun takeItem(material: Material, amount: Int = 1): Boolean =
        takeMatching(amount) { it.type == material }

    fun takeItem(item: ItemStack, amount: Int = item.amount): Boolean =
        takeMatching(amount) { it.isSimilar(item) }

    private fun countMatching(matches: (ItemStack) -> Boolean): Int =
        inventory().storageContents.sumOf { if (!it.isVacant() && matches(it!!)) it.amount else 0 }

    private fun takeMatching(amount: Int, matches: (ItemStack) -> Boolean): Boolean {
        val inventory = inventory()
        requirePositive(amount)
        val original = inventory.storageContents
        val updated = original.copyOf()
        var remaining = amount
        for ((slot, current) in original.withIndex()) {
            if (current.isVacant() || !matches(current!!)) continue
            val removed = minOf(remaining, current.amount)
            updated[slot] = if (removed == current.amount) null
                else current.clone().apply { this.amount -= removed }
            remaining -= removed
            if (remaining == 0) break
        }
        if (remaining > 0) return false
        applyChanges(inventory, original, updated)
        return true
    }

    private fun inventory(): PlayerInventory {
        MenuEngine.requireSync()
        return player.inventory
    }

    private fun stackLimit(item: ItemStack, inventory: PlayerInventory): Int =
        minOf(item.maxStackSize, inventory.maxStackSize)

    private fun applyChanges(inventory: PlayerInventory, original: Array<ItemStack?>, updated: Array<ItemStack?>) {
        for (slot in original.indices) {
            if (original[slot] !== updated[slot]) inventory.setItem(slot, updated[slot])
        }
    }

    private fun requirePositive(amount: Int) {
        require(amount > 0) { "Количество предметов должно быть положительным" }
    }

    private fun ItemStack?.isVacant(): Boolean = this == null || isEmpty
}
