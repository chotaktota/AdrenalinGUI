# Страницы и история

MultiGUISurface объединяет главную страницу, категории и переходы в одном классе. Каждая страница имеет свой кэш.

## Именованные страницы

~~~kotlin
class GuideGUI(plugin: JavaPlugin) : MultiGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        page("main") {
            with(newPattern(".........\n....E....\n.........")) {
                title("<red>Помощь")
                iconAt(pos('E')) {
                    type(Material.GOLD_INGOT)
                    name("<yellow>Экономика")
                    onClick { openPage("economy") }
                }
            }
        }

        page("economy", rows = 1) {
            with(newPattern("....B....")) {
                title("<yellow>Экономика")
                iconAt(pos('B')) {
                    type(Material.ARROW)
                    name("<white>Назад")
                    back()
                }
            }
        }
    }
}
~~~

По умолчанию начальная страница называется main. Другой идентификатор передаётся в конструктор MultiGUISurface через initialPage. Для открытия конкретной страницы снаружи используйте menu.open(player, "economy").

## Возврат

~~~kotlin
onClick {
    if (hasPreviousMenu()) back()
    else close()
}
~~~

История учитывает переходы между страницами и поверхностями. Хранится до 32 предыдущих назначений. Повторное открытие того же назначения не дублирует историю.

При отключении плагина его меню исключаются из истории.

## Жизненный цикл страницы

~~~kotlin
override fun onOpen(player: Player, page: String) {
    player.sendMessage("Открыта страница " + page)
}

override fun onClose(player: Player, page: String) {
    player.sendMessage("Закрыта страница " + page)
}
~~~

При переходе вызывается onClose старой страницы и onOpen новой. Поля старой страницы возвращают предметы.

## Обновление кэша

invalidateCache("economy") сбрасывает кэш конкретной страницы. invalidateCache() сбрасывает реестр и кэши страниц. Новое описание используется при следующем открытии.
