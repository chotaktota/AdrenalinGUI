# Примеры меню

Полные классы для своего плагина. Скопируйте выбранный класс в пакет example и зарегистрируйте его экземпляр через engine.menus.

## Помощь с категориями

Один MultiGUISurface содержит главную страницу и три категории.

~~~kotlin HelpGUI.kt
package example

import dev.aethereal.agui.surface.MultiGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class HelpGUI(plugin: JavaPlugin) : MultiGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        page("main") {
            with(newPattern("""
                .........
                ..E.M.S..
                .........
            """.trimIndent())) {
                title("<gold>Помощь")
                iconAt(pos('E')) {
                    type(Material.GOLD_INGOT)
                    name("<yellow>Экономика")
                    loreLine("<gray>Как зарабатывать и торговать")
                    allFlags()
                    onClick { openPage("economy") }
                }
                iconAt(pos('M')) {
                    type(Material.REDSTONE)
                    name("<red>Механики")
                    loreLine("<gray>Возможности сервера")
                    allFlags()
                    onClick { openPage("mechanics") }
                }
                iconAt(pos('S')) {
                    type(Material.GRASS_BLOCK)
                    name("<green>О сервере")
                    loreLine("<gray>Правила и полезная информация")
                    allFlags()
                    onClick { openPage("server") }
                }
            }
        }
        section("economy", "<yellow>Экономика", Material.GOLD_INGOT,
            "<gray>Зарабатывайте монеты и торгуйте с игроками.")
        section("mechanics", "<red>Механики", Material.REDSTONE,
            "<gray>Здесь можно описать механики вашего сервера.")
        section("server", "<green>О сервере", Material.GRASS_BLOCK,
            "<gray>Здесь можно разместить правила и контакты.")
    }

    private fun section(id: String, caption: String, material: Material, description: String) {
        page(id) {
            with(newPattern("""
                .........
                ....I....
                ....B....
            """.trimIndent())) {
                title(caption)
                iconAt(pos('I')) {
                    type(material)
                    name(caption)
                    loreLine(description)
                    allFlags()
                }
                iconAt(pos('B')) {
                    type(Material.ARROW)
                    name("<yellow>Назад")
                    loreLine("<gray>Вернуться к категориям")
                    allFlags()
                    onClick { openPage("main") }
                }
            }
        }
    }
}
~~~

## Проверка предмета с барьером

Меню принимает алмазный меч, обновляет доступность кнопки и показывает исходный предмет в обработчике.

~~~kotlin ItemCheckGUI.kt
package example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.plugin.java.JavaPlugin

class ItemCheckGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ..X.I.B..
            .........
        """.trimIndent())) {
            title("<red>Проверка предмета")
            val source = pos('X').first()
            val input = pos('I').first()
            val button = pos('B').first()

            inputAt(source, input) {
                placeholder {
                    type(Material.BARRIER)
                    name("<red>Положите алмазный меч")
                    loreLine("<gray>ЛКМ — стак, ПКМ — один")
                }
                acceptIf { item -> item.type == Material.DIAMOND_SWORD }
                loreLine("<gray>Предмет в поле проверки")
                onInsert { sound(Sound.BLOCK_ANVIL_PLACE, volume = 0.5f) }
                onTake { sound(Sound.ENTITY_ITEM_PICKUP) }
                onChange { refresh(button) }
            }

            iconAt(button) {
                type(Material.ANVIL)
                name("<green>Проверить предмет")
                enabledIf("<red>Сначала положите алмазный меч") { viewer ->
                    viewer.openInventory.topInventory.getItem(input)?.type == Material.DIAMOND_SWORD
                }
                disabled { reason ->
                    name("<gray>Проверить предмет")
                    lore(reason)
                }
                onClick {
                    val item = inputItem(input) ?: return@onClick
                    player.sendMessage("В поле находится " + item.type.name)
                    sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
                }
            }
        }
    }
}
~~~

## Профиль с плейсхолдерами

Для токенов профиля нужны PlaceholderAPI и соответствующие расширения.

~~~kotlin PlayerProfileGUI.kt
package example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class PlayerProfileGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ....P....
            .........
        """.trimIndent())) {
            title("<gold>Профиль %player_name%")
            iconAt(pos('P')) {
                type(Material.PAPER)
                name("<white>%player_name%")
                loreLine("<gray>Баланс: <yellow>%vault_eco_balance%")
                loreLine("<green>Нажмите, чтобы закрыть")
                allFlags()
                onClick { close() }
            }
        }
    }
}
~~~

## Анимация

Шаги скрывают, показывают, изменяют и заменяют кнопку.

~~~kotlin AnimatedGUI.kt
package example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class AnimatedGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern(".........\n....X....\n.........")) {
            title("<red>Анимация")
            iconAt(pos('X')) {
                type(Material.PAPER)
                name("<white>Нажмите")
                onClick { close() }
            }
            animation {
                at(0) { hide(pos('X')) }
                at(10) { show(pos('X')) }
                at(20) {
                    change(pos('X')) { player ->
                        name("<gold>${player.name}")
                        lore("&7Баланс: %vault_eco_balance%")
                        glow()
                    }
                }
                at(30) {
                    set(pos('X')) {
                        type(Material.EMERALD)
                        name("<green>Готово")
                        onClick { close() }
                    }
                }
                loop(60)
            }
        }
    }
}
~~~
