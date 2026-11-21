package dev.aethereal.benchmark

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.surface.MultiGUISurface
import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.plugin.Plugin

private data class WarpArgs(val material: Material, val label: String)

internal fun verifyInteraction(plugin: Plugin, viewer: Player) {
    val engine = (plugin.server.pluginManager.getPlugin("AdrenalinGUI") as AdrenalinGUI).engine
    val target = object : SingleGUISurface(2, plugin) {
        override fun onUpdateNodes() {
            with(newPattern(".........\n.........")) {
                iconAt(0) { type(Material.ARROW); back() }
            }
        }
    }
    val menu = object : MultiGUISurface(1, plugin) {
        override fun onUpdateNodes() {
            page("main") {
                with(newPattern("XY.......")) {
                    template("warpTemplate", WarpArgs::class.java) { args ->
                        type(args.material)
                        name(args.label)
                        loreLine("<gold>Общее описание")
                        glow()
                    }
                    val warp = template("warpTemplate", WarpArgs::class.java)
                    iconAt(pos('X'), warp, WarpArgs(Material.DIAMOND, "Первый")) {
                        onClick { openPage("other") }
                    }
                    iconAt(pos('Y'), warp, WarpArgs(Material.EMERALD, "Второй"))
                }
            }
            page("other") {
                with(newPattern(".........")) {
                    iconAt(0) { type(Material.BOOK); onClick { target.open(player) } }
                    iconAt(1) {
                        type(Material.PAPER)
                        visibleIf(engine::hasPreviousMenu)
                        onClick {
                            check(hasPreviousMenu())
                            inputChat("Тест: введите сумму", Int::class.javaObjectType) {
                                validate { it > 0 }
                                onFailure {
                                    check(Bukkit.isPrimaryThread())
                                    sendMessage("Тест: ошибка ${it.name}")
                                }
                                onSuccess {
                                    check(Bukkit.isPrimaryThread())
                                    check(it == 27)
                                    sendMessage("Тест: получено $it")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    fun click(slot: Int) {
        plugin.server.pluginManager.callEvent(InventoryClickEvent(viewer.openInventory,
            InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL))
    }
    fun later(delay: Long, action: () -> Unit) =
        plugin.server.scheduler.runTaskLater(plugin, Runnable { action() }, delay)

    menu.open(viewer)
    val initial = viewer.openInventory.topInventory
    check(!engine.hasPreviousMenu(viewer) && !engine.back(viewer))
    val first = checkNotNull(initial.getItem(0))
    val second = checkNotNull(initial.getItem(1))
    check(first.type == Material.DIAMOND && second.type == Material.EMERALD)
    check(first.itemMeta.lore() == second.itemMeta.lore() && first.itemMeta.enchantmentGlintOverride == true)
    check(first.itemMeta.customName() != second.itemMeta.customName())
    click(0)
    later(3) {
        check(engine.hasPreviousMenu(viewer))
        click(0)
    }
    later(6) {
        check(viewer.openInventory.topInventory.size == 18 && engine.hasPreviousMenu(viewer))
        click(0)
    }
    later(9) {
        check(viewer.openInventory.topInventory.getItem(0)?.type == Material.BOOK)
        check(engine.back(viewer))
        check(viewer.openInventory.topInventory.getItem(0)?.type == Material.DIAMOND)
        check(!engine.hasPreviousMenu(viewer))
        click(0)
    }
    later(12) { click(1) }
}
