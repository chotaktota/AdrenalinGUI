package dev.aethereal.benchmark

import dev.aethereal.agui.AdrenalinGUI
import dev.aethereal.agui.surface.SingleGUISurface
import dev.aethereal.agui.surface.MultiGUISurface
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.java.JavaPlugin

internal class GuisVerification(private val plugin: JavaPlugin, private val viewer: Player, private val status: (String) -> Unit) : AutoCloseable {
    private val engine = (plugin.server.pluginManager.getPlugin("AdrenalinGUI") as AdrenalinGUI).engine
    private val ids = (0 until 46).map { "verify:" + it.toString().padStart(2, '0') } + "verify:zz"
    private var builds = 0
    private var attachment: PermissionAttachment? = null
    fun start() {
        viewer.closeInventory()
        permission(false)
        for (id in ids.dropLast(1)) engine.menus.register(id, object : SingleGUISurface(1, plugin) {
            override fun onUpdateNodes() {
                builds++
                with(newPattern("X........")) { iconAt(0) { type(Material.DIAMOND) } }
            }
        })
        engine.menus.register(ids.last(), object : MultiGUISurface(1, plugin) {
            override fun onUpdateNodes() {
                builds++
                page("main") { with(newPattern("X........")) { iconAt(0) { type(Material.PAPER) } } }
            }
        })
        status("Готово: 47 меню; построений=$builds")
    }
    fun permission(value: Boolean) {
        attachment?.let { viewer.removeAttachment(it) }
        attachment = viewer.addAttachment(plugin, "adrenalingui.admin", value)
        check(viewer.hasPermission("adrenalingui.admin") == value)
    }
    fun report(): String = "Меню: ${ids.count { engine.menus.find(it) != null }}; построений=$builds"
    override fun close() {
        attachment?.let { viewer.removeAttachment(it) }
        attachment = null
        ids.forEach { engine.menus.unregister(it) }
    }
}
