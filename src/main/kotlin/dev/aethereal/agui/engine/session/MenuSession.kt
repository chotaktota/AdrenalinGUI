package dev.aethereal.agui.engine.session

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.engine.cache.CachedNode
import dev.aethereal.agui.surface.item.click.GUIClick
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import java.util.logging.Level

internal class MenuSession(
    val engine: MenuEngine,
    val player: Player,
    val destination: MenuDestination,
    history: List<MenuDestination>
) : InventoryHolder {
    val owner: Plugin get() = destination.surface.plugin
    private val template get() = destination.template
    var history = history
        private set
    val isActive: Boolean get() = active

    fun forget(plugin: Plugin) {
        history = history.filterNot { it.surface.plugin === plugin }
    }

    private val text = engine.text.forPlayer(player)
    private val contents = owner.server.createInventory(this, template.size, template.title(text))
    private val animated = template.animation?.let { template.animatedContents() }
    private val cursor = template.animation?.cursor()
    private var animationTask: dev.aethereal.agui.engine.MenuScheduler.Handle? = null
    private var active = false
    private var queued = false
    private val lastClicks = java.util.IdentityHashMap<CachedNode, Int>()
    private var returned = false
    private val linkedInputs = template.linkedInputs.map { LinkedInputSession(it, contents) }
    private val linkedSlots = linkedInputs.flatMap { listOf(it.definition.source to it, it.definition.target to it) }.toMap()

    fun linkedClick(slot: Int, click: ClickType): Boolean {
        val field = linkedSlots[slot] ?: return false
        if (!active || player.openInventory.topInventory !== contents) return true
        try {
            field.click(slot, click, player, engine.text.forPlayer(player))
        } catch (exception: Exception) {
            owner.logger.log(Level.SEVERE, "Ошибка обработки поля для предметов", exception)
            close()
        }
        return true
    }

    fun isLinked(slot: Int): Boolean = slot in linkedSlots

    fun shiftInput(rawSlot: Int) {
        if (!active || player.openInventory.topInventory !== contents) return
        val view = player.openInventory
        val sourceInventory = view.getInventory(rawSlot) ?: return
        val sourceSlot = view.convertSlot(rawSlot)
        val item = sourceInventory.getItem(sourceSlot)?.clone() ?: return
        val target = if (rawSlot < contents.size) player.inventory else contents
        val slots = if (target === contents) template.inputSlots.filterNot { isLinked(it) }
            else (0 until player.inventory.storageContents.size).toList()
        var remaining = item.amount
        for (slot in slots) {
            val current = target.getItem(slot) ?: continue
            if (!current.isSimilar(item)) continue
            val added = minOf(remaining, (minOf(item.maxStackSize, target.maxStackSize) - current.amount).coerceAtLeast(0))
            if (added == 0) continue
            target.setItem(slot, current.clone().apply { amount += added })
            remaining -= added
            if (remaining == 0) break
        }
        if (remaining > 0) {
            for (slot in slots) {
                if (target.getItem(slot)?.isEmpty == false) continue
                val added = minOf(remaining, item.maxStackSize, target.maxStackSize)
                target.setItem(slot, item.clone().apply { amount = added })
                remaining -= added
                if (remaining == 0) break
            }
        }
        sourceInventory.setItem(sourceSlot, if (remaining == 0) null else item.apply { amount = remaining })
    }

    fun isInput(slot: Int): Boolean = slot in template.inputSlots

    fun inputItem(slot: Int): org.bukkit.inventory.ItemStack? {
        MenuEngine.requireSync()
        require(isInput(slot)) { "Слот не является полем для предметов" }
        check(active && player.openInventory.topInventory === contents) { "Меню закрыто" }
        return linkedSlots[slot]?.item() ?: if (isLinked(slot)) null else contents.getItem(slot)?.clone()
    }

    fun takeInput(slot: Int): org.bukkit.inventory.ItemStack? {
        val item = inputItem(slot)
        val linked = linkedSlots[slot]
        if (linked != null) return linked.take(engine.text.forPlayer(player))
        contents.setItem(slot, null)
        return item
    }

    private fun returnInputs() {
        if (returned) return
        returned = true
        val linkedItems = linkedInputs.mapNotNull { it.drain() }
        val items = linkedItems + template.inputSlots.filterNot { isLinked(it) }.mapNotNull { slot ->
            contents.getItem(slot)?.also { contents.setItem(slot, null) }
        }
        for (item in items) {
            for (leftover in player.inventory.addItem(item).values) {
                player.world.dropItem(player.location, leftover).owner = player.uniqueId
            }
        }
    }
    private val lifecycle = SessionLifecycle(
        { notify("onOpen", destination.onOpen) }, { notify("onClose", destination.onClose) }
    )
    init {
        template.populate(contents, text)
        linkedInputs.forEach { it.render(text) }
    }
    override fun getInventory(): Inventory = contents
    fun open() {
        engine.started(this)
        active = true
        try {
            player.openInventory(contents)
            if (active && player.openInventory.topInventory === contents) {
                lifecycle.open()
                if (active && player.openInventory.topInventory === contents) nextFrame()
            }
        }
        finally { if (player.openInventory.topInventory !== contents) dispose() }
    }
    fun refresh(slots: IntArray) {
        MenuEngine.requireSync()
        if (!active || player.openInventory.topInventory !== contents) return
        val targets = slots.filterNot { isInput(it) || isLinked(it) }.toIntArray()
        val context = engine.text.forPlayer(player)
        if (animated == null) template.refresh(contents, context, targets) else animated.refresh(contents, context, targets)
        linkedInputs.forEach { it.render(context) }
    }
    fun dispatch(slot: Int, click: ClickType) {
        if (queued || !active || !owner.isEnabled) return
        val node = (if (animated == null) template.nodeAt(slot) else animated.nodeAt(slot)) ?: return
        val handler = node.handler ?: return
        if (!canClick(slot, node)) return
        val now = org.bukkit.Bukkit.getCurrentTick()
        if (node.cooldownTicks > 0 && lastClicks[node]?.let { now - it < node.cooldownTicks } == true) return
        val revision = animated?.revision(slot)
        queued = true
        engine.tasks.later(1L) {
            queued = false
            if (active && owner.isEnabled && player.isOnline && player.openInventory.topInventory === contents) {
                if (animated?.revision(slot) != revision || !canClick(slot, node)) return@later
                try {
                    val navigate = destination.onNavigate?.let { action ->
                        { target: String ->
                            if (active && owner.isEnabled && player.isOnline &&
                                player.openInventory.topInventory === contents) action(target)
                        }
                    }
                    if (node.cooldownTicks > 0) lastClicks[node] = org.bukkit.Bukkit.getCurrentTick()
                    handler.invoke(GUIClick(player, slot, click, destination.page, navigate, this))
                }
                catch (exception: Exception) {
                    owner.logger.log(Level.SEVERE, "Ошибка обработки клика в слоте $slot", exception)
                }
            }
        }
    }
    private fun canClick(slot: Int, node: CachedNode): Boolean {
        if (contents.getItem(slot)?.isEmpty != false) return false
        return try {
            node.isVisible(player)
        } catch (exception: Exception) {
            owner.logger.log(Level.SEVERE, "Ошибка проверки видимости иконки в слоте $slot", exception)
            false
        }
    }

    private fun nextFrame() {
        val next = cursor?.next() ?: return
        if (next.first == 0L) {
            runFrame(next.second)
            return
        }
        animationTask = engine.tasks.later(next.first) {
            animationTask = null
            runFrame(next.second)
        }
    }
    private fun runFrame(operations: List<dev.aethereal.agui.engine.cache.CachedAnimation.Operation>) {
        if (!active || !owner.isEnabled || !player.isOnline || player.openInventory.topInventory !== contents) return
        try {
            val slots = checkNotNull(animated).apply(operations)
            refresh(slots)
            if (active && player.openInventory.topInventory === contents) nextFrame()
        } catch (exception: Exception) {
            animationTask?.cancel()
            animationTask = null
            owner.logger.log(Level.SEVERE, "Ошибка анимации меню", exception)
            close()
        }
    }
    fun dispose() {
        returnInputs()
        active = false
        animationTask?.cancel()
        animationTask = null
        engine.finished(this)
        lifecycle.close()
    }
    fun close() {
        try {
            if (player.openInventory.topInventory === contents) player.closeInventory()
        } finally { dispose() }
    }

    private fun notify(name: String, callback: () -> Unit) {
        try { callback() }
        catch (exception: Exception) {
            owner.logger.log(Level.SEVERE, "Ошибка обработчика меню $name", exception)
        }
    }
}
