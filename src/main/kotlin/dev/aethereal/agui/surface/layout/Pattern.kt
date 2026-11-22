package dev.aethereal.agui.surface.layout

import dev.aethereal.agui.surface.animation.MenuAnimation
import dev.aethereal.agui.surface.item.Icon
import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.text.MenuText

class IconTemplate<T : Any> internal constructor(
    internal val type: Class<T>,
    internal val configure: Icon.(T) -> Unit
)

class IconTemplate2<A : Any, B : Any> internal constructor(
    internal val firstType: Class<A>,
    internal val secondType: Class<B>,
    internal val configure: Icon.(A, B) -> Unit
)

class Pattern internal constructor(layout: String, rows: Int) {
    private val positions: Map<Char, List<Int>>
    internal val size = rows * 9
    internal var caption = MenuText("")
    internal var animation: MenuAnimation? = null
    fun animation(configure: MenuAnimation.() -> Unit) {
        MenuEngine.requireSync()
        check(animation == null) { "Для шаблона можно задать только одну анимацию" }
        animation = MenuAnimation(size).apply(configure)
    }
    internal val linkedInputs = linkedMapOf<Int, Pair<Int, dev.aethereal.agui.surface.item.InputField>>()
    fun inputAt(slot: Int, target: Int, configure: dev.aethereal.agui.surface.item.InputField.() -> Unit) {
        MenuEngine.requireSync()
        require(slot in 0 until size && target in 0 until size && slot != target) {
            "Источник и целевой слот должны различаться и находиться в пределах меню"
        }
        require(slot !in linkedInputs && linkedInputs.values.none { it.first == target }) {
            "Источник или целевой слот уже используется полем"
        }
        require(target !in inputs) { "Целевой слот уже объявлен полем" }
        val field = dev.aethereal.agui.surface.item.InputField().apply(configure)
        linkedInputs[slot] = target to field
        inputs.add(target)
    }

    internal val inputEvents = linkedMapOf<Int, dev.aethereal.agui.surface.item.InputEvents>()
    internal val inputs = linkedSetOf<Int>()
    fun inputAt(slots: Iterable<Int>, configure: dev.aethereal.agui.surface.item.InputEvents.() -> Unit) {
        MenuEngine.requireSync()
        val targets = slots.toList()
        require(targets.none { target -> linkedInputs.values.any { it.first == target } }) {
            "Целевой слот уже используется связанным полем"
        }
        inputAt(targets)
        val events = dev.aethereal.agui.surface.item.InputEvents().apply(configure)
        targets.forEach { inputEvents[it] = events }
    }
    fun inputAt(slot: Int, configure: dev.aethereal.agui.surface.item.InputEvents.() -> Unit) =
        inputAt(listOf(slot), configure)

    fun inputAt(slots: Iterable<Int>) {
        MenuEngine.requireSync()
        val targets = slots.toList()
        require(targets.isNotEmpty() && targets.all { it in 0 until size }) { "Укажите слоты в пределах меню" }
        inputs.addAll(targets)
    }
    fun inputAt(slot: Int) = inputAt(listOf(slot))

    internal val icons = linkedMapOf<Int, Icon>()
    init {
        val lines = layout.trimIndent().lines()
        require(lines.size == rows && lines.all { it.length == 9 }) {
            "Шаблон должен содержать $rows строк по 9 символов"
        }
        positions = lines.joinToString("").withIndex().filter { it.value != '.' }
            .groupBy({ it.value }, { it.index })
    }
    fun pos(symbol: Char): List<Int> = requireNotNull(positions[symbol]) { "Символ отсутствует в шаблоне: $symbol" }
    fun title(value: String) {
        MenuEngine.requireSync()
        caption = MenuText(value)
    }
    fun title(value: () -> String) = title(value())
    private val templates = mutableMapOf<String, Any>()

    fun template(name: String, configure: Icon.() -> Unit): IconTemplate<Unit> =
        template(name, Unit::class.java) { configure() }

    fun <T : Any> template(name: String, type: Class<T>, configure: Icon.(T) -> Unit): IconTemplate<T> =
        registerTemplate(name, IconTemplate(type.kotlin.javaObjectType, configure))

    fun <A : Any, B : Any> template(
        name: String,
        firstType: Class<A>,
        secondType: Class<B>,
        configure: Icon.(A, B) -> Unit
    ): IconTemplate2<A, B> = registerTemplate(
        name, IconTemplate2(firstType.kotlin.javaObjectType, secondType.kotlin.javaObjectType, configure)
    )

    private fun <T : Any> registerTemplate(name: String, template: T): T {
        MenuEngine.requireSync()
        require(name.isNotBlank()) { "Имя шаблона не может быть пустым" }
        require(name !in templates) { "Шаблон иконки уже объявлен: $name" }
        templates[name] = template
        return template
    }

    fun template(name: String): IconTemplate<Unit> = template(name, Unit::class.java)

    fun <T : Any> template(name: String, type: Class<T>): IconTemplate<T> {
        val template = findTemplate(name)
        require(template is IconTemplate<*> && template.type == type.kotlin.javaObjectType) {
            "Шаблон $name не принимает параметры типа ${type.simpleName}"
        }
        @Suppress("UNCHECKED_CAST")
        return template as IconTemplate<T>
    }

    fun <A : Any, B : Any> template(name: String, firstType: Class<A>, secondType: Class<B>): IconTemplate2<A, B> {
        val template = findTemplate(name)
        require(template is IconTemplate2<*, *> &&
            template.firstType == firstType.kotlin.javaObjectType && template.secondType == secondType.kotlin.javaObjectType) {
            "Шаблон $name не принимает параметры типов ${firstType.simpleName} и ${secondType.simpleName}"
        }
        @Suppress("UNCHECKED_CAST")
        return template as IconTemplate2<A, B>
    }

    private fun findTemplate(name: String): Any {
        MenuEngine.requireSync()
        return requireNotNull(templates[name]) { "Шаблон иконки не найден: $name" }
    }

    fun iconAt(slots: Iterable<Int>, template: IconTemplate<Unit>, configure: Icon.() -> Unit = {}) =
        iconAt(slots, template, Unit, configure)

    fun iconAt(slot: Int, template: IconTemplate<Unit>, configure: Icon.() -> Unit = {}) =
        iconAt(listOf(slot), template, configure)

    fun <T : Any> iconAt(slots: Iterable<Int>, template: IconTemplate<T>, args: T, configure: Icon.() -> Unit = {}) =
        iconAt(slots) {
            template.configure(this, args)
            configure()
        }

    fun <T : Any> iconAt(slot: Int, template: IconTemplate<T>, args: T, configure: Icon.() -> Unit = {}) =
        iconAt(listOf(slot), template, args, configure)

    fun <A : Any, B : Any> iconAt(
        slots: Iterable<Int>,
        template: IconTemplate2<A, B>,
        first: A,
        second: B,
        configure: Icon.() -> Unit = {}
    ) = iconAt(slots) {
        template.configure(this, first, second)
        configure()
    }

    fun <A : Any, B : Any> iconAt(
        slot: Int,
        template: IconTemplate2<A, B>,
        first: A,
        second: B,
        configure: Icon.() -> Unit = {}
    ) = iconAt(listOf(slot), template, first, second, configure)

    fun iconAt(slots: Iterable<Int>, configure: Icon.() -> Unit) {
        MenuEngine.requireSync()
        val targets = slots.toList()
        require(targets.all { it in 0 until size }) { "Слот находится за пределами меню" }
        val icon = Icon().apply(configure)
        targets.forEach { icons[it] = icon }
    }
    fun iconAt(slot: Int, configure: Icon.() -> Unit) = iconAt(listOf(slot), configure)
}
