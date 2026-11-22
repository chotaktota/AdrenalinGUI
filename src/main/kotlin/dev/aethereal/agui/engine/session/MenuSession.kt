package dev.aethereal.agui.engine.session

import dev.aethereal.agui.surface.item.click.action.OtherAction

import dev.aethereal.agui.surface.item.InputChange

import dev.aethereal.agui.surface.item.InputCause

import org.bukkit.inventory.ItemStack

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
    private val linkedInputs = template.linkedInputs.map { definition ->
        LinkedInputSession(definition, contents) { acceptInput(definition.target, it) }
    }
    private val observedInputs = linkedMapOf<Int, ItemStack?>()
    private var observationQueued = false
    private var inputEventsQueued = false
    private val inputEvents = java.util.ArrayDeque<() -> Unit>()

    fun acceptInput(slot: Int, item: ItemStack): Boolean {
        if (item.isEmpty) return true
        return try {
            template.inputEvents[slot]?.accepts(player, item) ?: true
        } catch (exception: Exception) {
            owner.logger.log(Level.SEVERE, "Ошибка проверки предмета для поля $slot", exception)
            false
        }
    }

    fun observeInput(slot: Int) {
        if (template.inputEvents[slot]?.observes != true || isLinked(slot)) return
        if (slot !in observedInputs) observedInputs[slot] = contents.getItem(slot)?.takeUnless { it.isEmpty }?.clone()
        if (observationQueued) return
        observationQueued = true
        engine.tasks.later(1L) {
            observationQueued = false
            flushInputChanges()
        }
    }

    fun flushInputChanges() {
        val changes = observedInputs.toMap()
        observedInputs.clear()
        for ((slot, previous) in changes) {
            recordInput(slot, previous, contents.getItem(slot), InputCause.PLAYER)
        }
    }

    private fun recordInput(
        slot: Int,
        before: ItemStack?,
        after: ItemStack?,
        cause: InputCause
    ) {
        val rules = template.inputEvents[slot] ?: return
        if (!rules.observes) return
        val previous = before?.takeUnless { it.isEmpty }?.clone()
        val current = after?.takeUnless { it.isEmpty }?.clone()
        if (previous == current) return
        val same = previous != null && current != null && previous.isSimilar(current)
        val removed = (previous?.amount ?: 0) - if (same) checkNotNull(current).amount else 0
        val inserted = (current?.amount ?: 0) - if (same) checkNotNull(previous).amount else 0
        fun enqueue(
            callback: (OtherAction.(InputChange) -> Unit)?,
            item: ItemStack?
        ) {
            if (callback == null) return
            val change = InputChange(slot, cause, previous, current, item)
            inputEvents.add {
                try {
                    val navigate = destination.onNavigate?.let { action ->
                        { target: String -> if (active && engine.currentSession(player) === this) action(target) }
                    }
                    callback.invoke(GUIClick(player, slot, ClickType.UNKNOWN, destination.page, navigate, this), change)
                } catch (exception: Exception) {
                    owner.logger.log(Level.SEVERE, "Ошибка обработчика поля $slot", exception)
                }
            }
        }
        if (removed > 0) enqueue(rules.taken, previous?.clone()?.apply { amount = removed })
        if (inserted > 0) enqueue(rules.inserted, current?.clone()?.apply { amount = inserted })
        enqueue(rules.changed, current ?: previous)
        if (inputEventsQueued || inputEvents.isEmpty()) return
        inputEventsQueued = true
        engine.tasks.later(1L) {
            inputEventsQueued = false
            val callbacks = inputEvents.toList()
            inputEvents.clear()
            if (owner.isEnabled && player.isOnline) callbacks.forEach { it() }
        }
    }
    private val linkedSlots = linkedInputs.flatMap { listOf(it.definition.source to it, it.definition.target to it) }.toMap()

    fun linkedClick(slot: Int, click: ClickType): Boolean {
        val field = linkedSlots[slot] ?: return false
        if (!active || player.openInventory.topInventory !== contents) return true
        val previous = field.item()
        try {
            field.click(slot, click, player, engine.text.forPlayer(player))
            recordInput(field.definition.target, previous, field.item(), InputCause.PLAYER)
        } catch (exception: Exception) {
            owner.logger.log(Level.SEVERE, "Ошибка обработки поля для предметов", exception)
            engine.tasks.later(1L) { if (active) close() }
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
        val previous = template.inputEvents.filterValues { it.observes }.keys.filterNot { isLinked(it) }.associateWith {
            contents.getItem(it)?.clone()
        }
        var remaining = item.amount
        for (slot in slots) {
            val current = target.getItem(slot) ?: continue
            if (!current.isSimilar(item)) continue
            val added = minOf(remaining, (minOf(item.maxStackSize, target.maxStackSize) - current.amount).coerceAtLeast(0))
            if (added == 0) continue
            val next = current.clone().apply { amount += added }
            if (target === contents && !acceptInput(slot, next)) continue
            target.setItem(slot, next)
            remaining -= added
            if (remaining == 0) break
        }
        if (remaining > 0) {
            for (slot in slots) {
                if (target.getItem(slot)?.isEmpty == false) continue
                val added = minOf(remaining, item.maxStackSize, target.maxStackSize)
                val next = item.clone().apply { amount = added }
                if (target === contents && !acceptInput(slot, next)) continue
                target.setItem(slot, next)
                remaining -= added
                if (remaining == 0) break
            }
        }
        sourceInventory.setItem(sourceSlot, if (remaining == 0) null else item.apply { amount = remaining })
        for ((slot, before) in previous) {
            recordInput(slot, before, contents.getItem(slot), InputCause.PLAYER)
        }
    }

    fun isInput(slot: Int): Boolean = slot in template.inputSlots

    fun inputItem(slot: Int): ItemStack? {
        MenuEngine.requireSync()
        require(isInput(slot)) { "Слот не является полем для предметов" }
        check(active && player.openInventory.topInventory === contents) { "Меню закрыто" }
        return linkedSlots[slot]?.item() ?: if (isLinked(slot)) null else contents.getItem(slot)?.clone()
    }

    fun takeInput(slot: Int): ItemStack? {
        MenuEngine.requireSync()
        flushInputChanges()
        val item = inputItem(slot)
        val linked = linkedSlots[slot]
        val taken = if (linked != null) linked.take(engine.text.forPlayer(player)) else item.also {
            contents.setItem(slot, null)
        }
        recordInput(slot, taken, null, InputCause.TAKE_INPUT)
        return taken
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
                if (active && player.openInventory.topInventory === contents) {
                    if (template.conditionalSlots.isNotEmpty()) refresh(template.conditionalSlots)
                    nextFrame()
                }
            }
        }
        catch (exception: Exception) {
            close()
            throw exception
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
        val handler = node.handler
        if (handler == null && !node.hasConditions) return
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
                    val action = GUIClick(player, slot, click, destination.page, navigate, this)
                    val reason = node.disabledReason(player)
                    if (reason != null) {
                        if (node.denied != null) node.denied.invoke(action, reason)
                        else if (reason.isNotBlank()) player.sendMessage(engine.text.forPlayer(player).render(dev.aethereal.agui.text.MenuText(reason)))
                        if (active) refresh(intArrayOf(slot))
                        return@later
                    }
                    handler?.invoke(action)
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
        if (engine.isRunning && owner.isEnabled) flushInputChanges() else observedInputs.clear()
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
