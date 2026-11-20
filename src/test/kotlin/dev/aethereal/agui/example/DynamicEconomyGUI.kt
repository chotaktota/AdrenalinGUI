package dev.aethereal.agui.example

import dev.aethereal.agui.menu.MenuEngine
import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration

class DynamicEconomyGUI(plugin: JavaPlugin, private val engine: MenuEngine) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern(".........\n....B....\n.........")) {
            title("<gold>Магазин")
            iconAt(pos('B')) {
                type(Material.PAPER)
                dynamic { player ->
                    val balance = engine.economy.balance(player)
                    name("<gold>${engine.economy.format(balance)}")
                    lore("&7Игрок: %player_name%", "<gray>Доступно: $balance")
                    glow(balance >= 100.0)
                    if (balance >= 100.0) enchant(Enchantment.UNBREAKING, 1)
                }
                onClick {
                    if (engine.economy.withdraw(player, 100.0).success) refresh(slot)
                }
            }
        }
    }

    fun grant(uuid: java.util.UUID) = engine.permissions.setPermission(uuid, "shop.vip", duration = Duration.ofDays(7))
}
