package dev.aethereal.agui.command

import dev.aethereal.agui.engine.MenuEngine
import dev.aethereal.agui.surface.GUISurface
import dev.aethereal.agui.surface.MultiGUISurface
import dev.aethereal.agui.surface.SingleGUISurface
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.util.logging.Level

internal class GuisCommand(private val engine: MenuEngine) : org.bukkit.command.TabExecutor {
    override fun onCommand(sender: CommandSender, command: org.bukkit.command.Command, label: String, args: Array<out String>) = execute(sender, args)
    override fun onTabComplete(sender: CommandSender, command: org.bukkit.command.Command, alias: String, args: Array<out String>) = suggest(sender, args)

    fun execute(sender: CommandSender, rawArgs: Array<out String>): Boolean {
        MenuEngine.requireSync()
        if (!sender.hasPermission(PERMISSION)) { reply(sender, "Нет права $PERMISSION", true); return true }
        val args = rawArgs.filter { it.isNotBlank() }
        if (args.isEmpty()) { usage(sender); return true }
        if (!args[0].equals("info", true)) { usage(sender); return true }
        try {
            when {
                args.size == 1 -> list(sender, 0)
                args.size == 3 && args[1] == "--page" -> {
                    val page = args[2].toIntOrNull()
                    if (page == null || page < 1) reply(sender, "Номер страницы должен быть положительным целым числом", true)
                    else list(sender, page - 1)
                }
                args.size == 2 -> {
                    val menu = engine.menus.find(args[1])
                    if (menu == null) reply(sender, "Меню не найдено: ${args[1]}", true) else info(sender, args[1], menu)
                }
                else -> usage(sender)
            }
        } catch (exception: Exception) {
            reply(sender, "Не удалось получить информацию. Подробности в журнале сервера", true)
            engine.plugin.logger.log(Level.SEVERE, "Ошибка команды /guis info", exception)
        }
        return true
    }
    fun suggest(sender: CommandSender, args: Array<out String>): List<String> {
        if (!sender.hasPermission(PERMISSION)) return emptyList()
        val candidates = when {
            args.size == 1 -> listOf("info")
            args.size == 2 && args[0].equals("info", true) -> engine.menus.entries().keys.toList() + "--page"
            else -> emptyList()
        }
        return candidates.filter { it.startsWith(args.lastOrNull() ?: "", true) }.sorted()
    }
    private fun list(sender: CommandSender, requested: Int) {
        val entries = engine.menus.entries().toList().sortedBy { it.first }
        if (entries.isEmpty()) { reply(sender, "Зарегистрированных меню нет"); return }
        val page = MenuListPage(entries, requested, 10)
        reply(sender, "Меню: ${entries.size} · страница ${page.index + 1}/${page.count}")
        page.entries.forEach { (id, menu) -> reply(sender, "$id · плагин: ${menu.plugin.name} · ${kind(menu)}") }
        if (page.count > 1) reply(sender, "Другая страница: /guis info --page <номер>")
    }
    private fun info(sender: CommandSender, id: String, menu: GUISurface) {
        reply(sender, "Меню: $id")
        reply(sender, "Плагин: ${menu.plugin.name} ${menu.plugin.pluginMeta.version}")
        reply(sender, "Состояние плагина: ${if (menu.plugin.isEnabled) "включён" else "отключён"}")
        reply(sender, "Тип: ${kind(menu)}")
        reply(sender, "Класс: ${menu.javaClass.name}")
        when (menu) {
            is MultiGUISurface -> reply(sender, "Начальная страница: ${menu.initialPage} · базовый размер: ${menu.rows} строк")
            is SingleGUISurface -> reply(sender, "Размер: ${menu.rows} строк")
        }
    }
    private fun kind(menu: GUISurface) = when (menu) {
        is MultiGUISurface -> "Многостраничное"
        is SingleGUISurface -> "Одностраничное"
        else -> "Пользовательское"
    }
    private fun usage(sender: CommandSender) = reply(sender, "/guis info [меню] · /guis info --page <номер>")
    private fun reply(sender: CommandSender, text: String, error: Boolean = false) {
        sender.sendMessage(Component.text("[AdrenalinGUI] ", NamedTextColor.DARK_RED)
            .append(Component.text(text, if (error) NamedTextColor.RED else NamedTextColor.GRAY)))
    }
    companion object { const val PERMISSION = "adrenalingui.admin" }
}
