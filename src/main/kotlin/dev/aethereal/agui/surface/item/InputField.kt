package dev.aethereal.agui.surface.item

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.text.MenuText
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import dev.aethereal.agui.surface.item.click.action.OtherAction

enum class InputTransfer { CLICK, ALL, ONE }

enum class InputCause { PLAYER, TAKE_INPUT }

class InputChange internal constructor(
    val slot: Int,
    val cause: InputCause,
    previous: ItemStack?,
    current: ItemStack?,
    item: ItemStack?
) {
    private val before = previous?.clone()
    private val after = current?.clone()
    private val changed = item?.clone()
    val previous: ItemStack? get() = before?.clone()
    val current: ItemStack? get() = after?.clone()
    val item: ItemStack? get() = changed?.clone()
}

open class InputEvents internal constructor() {
    internal var accepts: ((Player, ItemStack) -> Boolean)? = null
    internal var inserted: (OtherAction.(InputChange) -> Unit)? = null
    internal var taken: (OtherAction.(InputChange) -> Unit)? = null
    internal var changed: (OtherAction.(InputChange) -> Unit)? = null

    fun acceptIf(condition: (ItemStack) -> Boolean) = acceptIf { _, item -> condition(item) }

    fun acceptIf(condition: (Player, ItemStack) -> Boolean) {
        MenuEngine.requireSync()
        val previous = accepts
        accepts = { player, item ->
            (previous?.invoke(player, item) ?: true) && condition(player, item.clone())
        }
    }

    fun onInsert(action: OtherAction.(InputChange) -> Unit) {
        MenuEngine.requireSync()
        inserted = action
    }

    fun onTake(action: OtherAction.(InputChange) -> Unit) {
        MenuEngine.requireSync()
        taken = action
    }

    fun onChange(action: OtherAction.(InputChange) -> Unit) {
        MenuEngine.requireSync()
        changed = action
    }
}

class InputField internal constructor() : InputEvents() {
    internal var transferMode = InputTransfer.CLICK
    internal val lines = mutableListOf<MenuText>()
    internal val icon = Icon().apply { type(Material.BARRIER) }

    fun placeholder(configure: Icon.() -> Unit) {
        MenuEngine.requireSync()
        icon.configure()
    }

    fun loreLine(value: String) {
        MenuEngine.requireSync()
        lines.add(MenuText(value))
    }

    fun transfer(mode: InputTransfer) {
        MenuEngine.requireSync()
        transferMode = mode
    }
}
