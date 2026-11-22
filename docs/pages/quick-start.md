# Первое меню

Полный класс с одной кнопкой. Его экземпляр создаётся один раз и используется для всех открытий.

## Класс меню

~~~kotlin QuickStartGUI.kt
package example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class QuickStartGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ....X....
            .........
        """.trimIndent())) {
            title("<gradient:#55FFAA:#55AAFF>Welcome</gradient>")
            iconAt(pos('X')) {
                type(Material.ENDER_EYE)
                name("<white>Explore the server")
                loreLine("<gray>Your menu. Your Kotlin code.")
                loreLine("<green>Click to close")
                glow()
                allFlags()
                onClick { close() }
            }
        }
    }
}
~~~

## Регистрация и открытие

~~~kotlin MyPlugin.kt
package example

import dev.aethereal.agui.AdrenalinGUI
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        val engine = JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine
        engine.menus.register("main", QuickStartGUI(this))
        requireNotNull(getCommand("menu")).setExecutor { sender, _, _, _ ->
            if (sender is Player) engine.open("main", sender)
            true
        }
    }
}
~~~

Скопируйте оба класса в пакет example своего плагина. Команда menu объявляется в plugin.yml, как в разделе установки. AdrenalinGUI загружается раньше вашего плагина благодаря depend.

## Первое и повторное открытие

Первое открытие строит описание и прототип предмета. Игрок получает отдельный инвентарь с копией предмета. Нажатие кнопки выполнит close на следующем тике основного потока. Повторное открытие того же экземпляра использует построенный кэш.
