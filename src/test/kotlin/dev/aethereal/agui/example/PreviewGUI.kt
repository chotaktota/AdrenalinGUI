package dev.aethereal.agui.example

import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class PreviewGUI(plugin: JavaPlugin) : SingleGUISurface(6, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ..123456.
            ..789ABC.
            ..DEFGHI.
            ....J....
            ....K....
        """.trimIndent())) {
            title { "test 2" }
            iconAt(pos('1')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('2')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                loreLine("<#55FF55>Вперёд ▶")
                loreLine("")
                loreLine("<#555555>────────────────")
                loreLine("<#AAAAAA>◀ Назад")
                glow()
                allFlags()
            }
            iconAt(pos('3')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('4')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('5')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('6')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('7')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('8')) {
                type(Material.CHISELED_BOOKSHELF)
                name("<#FFFFFF>Резная книжная полка")
                allFlags()
            }
            iconAt(pos('9')) {
                type(Material.RED_STAINED_GLASS_PANE)
                name("<#FFFFFF>Красная стеклянная панель")
                allFlags()
            }
            iconAt(pos('A')) {
                type(Material.CHISELED_BOOKSHELF)
                name("<#FFFFFF>Резная книжная полка")
                allFlags()
            }
            iconAt(pos('B')) {
                type(Material.CHISELED_BOOKSHELF)
                name("<#FFFFFF>Резная книжная полка")
                allFlags()
            }
            iconAt(pos('C')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('D')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('E')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('F')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('G')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('H')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('I')) {
                type(Material.ENDER_EYE)
                name("<#FFFFFF>Око Эндера")
                allFlags()
            }
            iconAt(pos('J')) {
                type(Material.RED_STAINED_GLASS_PANE)
                name("<#FFFFFF>Красная стеклянная панель")
                allFlags()
            }
            iconAt(pos('K')) {
                type(Material.RED_STAINED_GLASS_PANE)
                name("<#FFFFFF>Красная стеклянная панель")
                loreLine("<#FFFF55>Нажмите, чтобы купить")
                loreLine("<#FFAA00>Цена: <#FFFFFF>100 монет")
                allFlags()
            }
        }
    }
}
