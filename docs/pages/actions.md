# Действия клика

Внутри onClick доступны игрок, вид клика, навигация, инвентарь, экономика, права, чат и звуки.

## Контекст обработчика

~~~kotlin
iconAt(pos('X')) {
    type(Material.BOOK)
    cooldown(10)
    onClick {
        if (isRightClick) {
            sound(Sound.UI_BUTTON_CLICK)
        } else {
            player.sendMessage("Вы нажали слот " + slot)
        }
    }
}
~~~

| Свойство | Значение |
| --- | --- |
| player | Игрок текущей сессии |
| slot | Индекс нажатого слота |
| click | Bukkit ClickType |
| isLeftClick, isRightClick, isShiftClick | Проверки вида клика |
| page | Идентификатор страницы для MultiGUISurface |

Обычная кнопка выполняет действие на следующем тике основного потока. Закрытое окно и устаревшая после анимации кнопка отбрасывают отложенный клик.

## Предметы игрока

~~~kotlin
onClick {
    val diamonds = countItem(Material.DIAMOND)
    if (hasItem(Material.DIAMOND, 3)) {
        player.sendMessage("Алмазов: " + diamonds)
    }
    if (!giveItem(Material.EMERALD, 20)) {
        player.sendMessage("В инвентаре недостаточно места")
    }
}
~~~

| Метод | Результат |
| --- | --- |
| countItem(material или stack) | Количество подходящих предметов |
| hasItem(..., amount) | Хватает ли указанного количества |
| takeItem(..., amount) | Списать полное количество или вернуть false |
| giveItem(..., amount) | Выдать полное количество или вернуть false |
| freeInventorySlots() | Число пустых слотов |
| isInventoryFull() | Заняты ли все слоты |
| spaceForItem(stack) | Вместимость с учётом неполных стаков |
| canFitItem(stack, amount) | Поместится ли указанное количество |

## Сравнение и атомарность

Material сравнивает тип. ItemStack сравнивает тип и метаданные через isSimilar. Его количество служит значением по умолчанию для операций с количеством.

В расчёт входят основной инвентарь и хотбар. giveItem и takeItem сначала рассчитывают полную операцию. При нехватке места или предметов возвращается false, содержимое сохраняется.

isInventoryFull показывает занятость слотов. canFitItem учитывает место в совместимых стаках, даже когда все слоты заняты.

## Действия вне меню

~~~kotlin
val actions = engine.actions(player)
val balance = actions.balance()

val inventory = ItemAction(player)
val count = inventory.countItem(Material.DIAMOND)
~~~

Используйте основной поток сервера. ItemAction находится в dev.aethereal.agui.surface.item.click.action.
