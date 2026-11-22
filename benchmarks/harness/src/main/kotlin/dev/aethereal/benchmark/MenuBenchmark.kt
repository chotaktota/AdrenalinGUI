package dev.aethereal.benchmark

import com.google.gson.GsonBuilder
import dev.aethereal.agui.surface.SingleGUISurface
import dev.by1337.bmenu.BMenu
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.java.JavaPlugin
import ru.abstractmenus.services.MenuManager
import java.io.File
import java.util.UUID

class MenuBenchmark : JavaPlugin(), Listener {
    private val menus = hashMapOf<Boolean, SingleGUISurface>()
    private val samples = arrayListOf<Sample>()
    private val pending = hashMapOf<UUID, Long>()
    private val states = hashMapOf<UUID, String>()
    private var phase = "warmup"
    private var failures = 0
    private var rulesStatus = "Не запускалась"
    private var inputStatus = "Не запускалась"
    private var animationStatus = "Не запускалась"
    private var guisStatus = "Не запускалась"
    private var guisVerification: GuisVerification? = null

    data class Sample(val phase: String, val menu: String, val metric: String, val nanos: Long)

    override fun onEnable() {
        object : PlaceholderExpansion() {
            override fun getIdentifier() = "bench"
            override fun getAuthor() = "chotaktota"
            override fun getVersion() = "1"
            override fun persist() = true
            override fun onRequest(player: OfflinePlayer?, params: String): String? = when(params) {
                "name" -> player?.name
                "value" -> "100"
                else -> null
            }
        }.register()
        for (dynamic in listOf(false, true)) menus[dynamic] = object : SingleGUISurface(6, this) {
            override fun onUpdateNodes() {
                with(newPattern(List(6) { "XXXXXXXXX" }.joinToString("\n"))) {
                    title(if (dynamic) "&6%bench_name%" else "&6Benchmark")
                    for (slot in 0 until 54) iconAt(slot) {
                        type(Material.PAPER)
                        name(if (dynamic) "&a%bench_name% $slot" else "&aItem $slot")
                        loreLine("&7Line one")
                        loreLine("&7Line two")
                        loreLine("&7Line three")
                        loreLine(if (dynamic) "&e%bench_value%" else "&e100")
                        onClick { player.performCommand("benchack") }
                    }
                }
            }
        }
        server.pluginManager.registerEvents(this, this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name) {
            "benchverifyrules" -> if (sender !is Player) {
                if (args.firstOrNull() != "status") {
                    rulesStatus = "Выполняется"
                    verifyInputRules(this, checkNotNull(server.getPlayerExact("Bench0"))) { rulesStatus = it }
                }
                sender.sendMessage(rulesStatus)
            }
            "benchverifyinputs" -> if (sender !is Player) {
                if (args.firstOrNull() != "status") {
                    inputStatus = "Выполняется"
                    verifyInputs(this, checkNotNull(server.getPlayerExact("Bench0"))) { inputStatus = it }
                }
                sender.sendMessage(inputStatus)
            }
            "benchverifyguis" -> if (sender !is Player) {
                guisStatus = "Выполняется"
                guisVerification?.close()
                guisVerification = GuisVerification(this, checkNotNull(server.getPlayerExact("Bench0"))) { guisStatus = it }
                guisVerification?.start()
                sender.sendMessage(guisStatus)
            }
            "benchguisstatus" -> if (sender !is Player) sender.sendMessage(guisVerification?.report() ?: guisStatus)
            "benchguispermission" -> if (sender !is Player) {
                checkNotNull(guisVerification).permission(args.firstOrNull() == "allow")
                sender.sendMessage("Право обновлено")
            }
            "benchguiscleanup" -> if (sender !is Player) {
                guisVerification?.close()
                guisVerification = null
                guisStatus = "Очищено"
                sender.sendMessage(guisStatus)
            }
            "benchverifyanimation" -> if (sender !is Player) {
                val viewer = checkNotNull(server.getPlayerExact(args.firstOrNull() ?: "Bench0"))
                animationStatus = "Выполняется"
                AnimationVerification(this, viewer) { animationStatus = it }.start()
                sender.sendMessage(animationStatus)
            }
            "benchanimationstatus" -> if (sender !is Player) sender.sendMessage(animationStatus)
            "benchverifyinteraction" -> if (sender !is Player) {
                val viewer = checkNotNull(server.getPlayerExact("Bench0"))
                verifyInteraction(this, viewer)
            }
            "benchverifyinventory" -> if (sender !is Player) {
                val viewer = checkNotNull(server.getPlayerExact("Bench0"))
                verifyPlayerItems(viewer, args.firstOrNull())
                sender.sendMessage("Инвентарь: проверка пройдена")
            }
            "benchverify" -> if (sender !is Player) {
                val player = server.getPlayerExact(args.firstOrNull() ?: "Bench0") ?: error("Тестовый игрок не найден")
                verifyDynamic(player)
                verifyItemProperties(this, player)
                sender.sendMessage("Динамические иконки: проверка пройдена")
            }
            "benchphase" -> {
                if (sender !is Player) {
                    phase = args.firstOrNull() ?: "warmup"
                    if (phase == "warmup" || phase == "smoke") {
                        samples.clear()
                        failures = 0
                        pending.clear()
                        states.clear()
                    }
                    sender.sendMessage("Фаза: $phase")
                }
            }
            "benchopen" -> if (sender is Player && args.size == 2) {
                val which = args[0]
                val dynamic = args[1] == "dynamic"
                val key = "$which/${args[1]}"
                pending.remove(sender.uniqueId)
                states[sender.uniqueId] = key
                val start = System.nanoTime()
                try {
                    when(which) {
                        "agui" -> menus.getValue(dynamic).open(sender)
                        "bmenu" -> BMenu.menuLoader().create("bench:${args[1]}", sender, null).open()
                        "abstract" -> {
                            val manager = MenuManager.instance()
                            manager.openMenu(sender, requireNotNull(manager.getMenu("bench_${args[1]}")))
                        }
                        else -> error("Неизвестный движок")
                    }
                    val elapsed = System.nanoTime() - start
                    val inventory = sender.openInventory.topInventory
                    check(inventory.size == 54) { "Неверный размер окна: ${inventory.size}" }
                    if (phase != "warmup") samples += Sample(phase, key, "open", elapsed)
                } catch (exception: Exception) {
                    failures++
                    logger.log(java.util.logging.Level.SEVERE, "Ошибка замера $key", exception)
                }
            }
            "benchack" -> if (sender is Player) {
                val start = pending.remove(sender.uniqueId)
                if (start != null && phase != "warmup") samples += Sample(phase,
                    states[sender.uniqueId] ?: "unknown", "click_action", System.nanoTime() - start)
            }
            "benchdump" -> if (sender !is Player) {
                val snapshot = samples.toList()
                val errors = failures
                server.scheduler.runTaskAsynchronously(this, Runnable {
                    val output = File(dataFolder, "results.json")
                    output.parentFile.mkdirs()
                    output.writeText(GsonBuilder().setPrettyPrinting().create().toJson(mapOf("samples" to snapshot, "failures" to errors)))
                    logger.info("Результаты сохранены: ${snapshot.size}; ошибок: $errors")
                })
            }
        }
        return true
    }

    private fun verifyDynamic(viewer: Player) {
        val engine = (server.pluginManager.getPlugin("AdrenalinGUI") as dev.aethereal.agui.AdrenalinGUI).engine
        var value = 1
        var opened = 0
        var closed = 0
        val menu = object : SingleGUISurface(1, this) {
            override fun onOpen(player: Player) { opened++ }
            override fun onClose(player: Player) { closed++ }
            override fun onUpdateNodes() {
                with(newPattern("XX.......")) {
                    title("Динамика")
                    iconAt(pos('X')) {
                        type(Material.PAPER)
                        dynamic { player ->
                            name("<green>${player.name}: $value")
                            lore("&eЗначение: $value", "%bench_name%")
                            glow(value == 1)
                            if (value == 1) enchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 2)
                        }
                    }
                }
            }
        }
        menu.open(viewer)
        val inventory = viewer.openInventory.topInventory
        val old = checkNotNull(inventory.getItem(0)).clone()
        check(old.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING) == 2)
        value = 2
        engine.refresh(viewer, 0)
        val changed = checkNotNull(inventory.getItem(0))
        check(changed != old && inventory.getItem(1) == old)
        check(!changed.containsEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING))
        check(changed.itemMeta.enchantmentGlintOverride == false)
        check(changed.itemMeta.customName() == net.kyori.adventure.text.Component.text("${viewer.name}: 2", net.kyori.adventure.text.format.NamedTextColor.GREEN)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
        engine.refresh(viewer)
        check(inventory.getItem(1) == changed)
        check(opened == 1 && closed == 0)
        viewer.closeInventory()
        menu.open(viewer)
        check(viewer.openInventory.topInventory.getItem(0) == changed)
        viewer.closeInventory()
        check(opened == 2 && closed == 2)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun click(event: InventoryClickEvent) {
        val id = event.whoClicked.uniqueId
        if (id in states && event.rawSlot in 0 until 54 && !pending.containsKey(id)) pending[id] = System.nanoTime()
    }
}
