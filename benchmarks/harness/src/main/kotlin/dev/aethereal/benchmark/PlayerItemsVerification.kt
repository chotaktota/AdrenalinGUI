package dev.aethereal.benchmark

import dev.aethereal.agui.surface.item.click.action.ItemAction
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal fun verifyPlayerItems(player: Player, customId: String?) {
    val inventory = player.inventory
    val original = inventory.storageContents.map { it?.clone() }.toTypedArray()
    val offhand = inventory.itemInOffHand.clone()
    val items = ItemAction(player)
    try {
        val special = ItemStack.of(Material.DIAMOND, 5)
        special.editMeta { it.setMaxStackSize(8) }
        inventory.storageContents = Array(original.size) { ItemStack.of(Material.DIRT, 64) }
        inventory.setItem(0, special)
        inventory.setItemInOffHand(special.clone())
        check(items.isInventoryFull() && items.freeInventorySlots() == 0)
        check(items.hasItem(Material.DIAMOND, 5) && items.countItem(special) == 5)
        check(items.spaceForItem(special) == 3 && items.canFitItem(special, 3))
        check(!items.giveItem(special, 4) && !items.takeItem(special, 6))
        check(items.countItem(special) == 5)
        check(items.giveItem(special, 3) && items.countItem(special) == 8)
        inventory.setItem(1, ItemStack.of(Material.DIAMOND, 2))
        check(items.countItem(Material.DIAMOND) == 10 && items.countItem(special) == 8)
        check(items.takeItem(special, 8) && items.countItem(Material.DIAMOND) == 2)
        inventory.setItem(1, null)
        check(!items.giveItem(special, 17) && items.freeInventorySlots() == 2)
        check(items.giveItem(special, 16) && items.countItem(special) == 16)
        check(items.takeItem(Material.DIAMOND, 10) && items.countItem(special) == 6)
        check(special.amount == 5 && inventory.itemInOffHand.amount == 5)
        if (customId != null) {
            inventory.storageContents = arrayOfNulls(original.size)
            val type = items.javaClass
            val amountType = Int::class.javaPrimitiveType
            val give = type.getMethod("giveItem", String::class.java, amountType)
            val take = type.getMethod("takeItem", String::class.java, amountType)
            val count = type.getMethod("countItem", String::class.java)
            check(give.invoke(items, customId, 3) == true)
            val first = checkNotNull(inventory.getItem(0)).clone()
            first.editMeta { it.setUnbreakable(true) }
            inventory.setItem(0, first)
            check(count.invoke(items, customId) == 3)
            check(take.invoke(items, customId, 4) == false && count.invoke(items, customId) == 3)
            check(take.invoke(items, customId, 2) == true && count.invoke(items, customId) == 1)
            check(give.invoke(items, "adrenalingui-missing-fixture", 1) == false)
        }
    } finally {
        inventory.storageContents = original
        inventory.setItemInOffHand(offhand)
    }
}
