package dev.aethereal.agui.example

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
