# Поля для предметов

Игрок помещает свои предметы в выбранные слоты. Оставшееся содержимое возвращается при закрытии и переходе страницы.

## Обычное поле

~~~kotlin
inputAt(pos('I')) {
    acceptIf { item -> item.type == Material.DIAMOND_SWORD }
    onInsert { change ->
        player.sendMessage("Добавлено: " + change.item?.amount)
        sound(Sound.BLOCK_ANVIL_PLACE, volume = 0.5f)
    }
    onTake { sound(Sound.ENTITY_ITEM_PICKUP) }
    onChange { refresh(pos('B').first()) }
}
~~~

Обычные поля поддерживают перенос курсором, shift-клик, клавиши хотбара и drag по разрешённым слотам. Фильтр проверяет итоговое содержимое слота, включая количество.

## Барьер и отдельный слот

~~~kotlin
val source = pos('X').first()
val target = pos('I').first()

inputAt(source, target) {
    placeholder {
        type(Material.BARRIER)
        name("<red>Положите алмазный меч")
        loreLine("<gray>ЛКМ — стак, ПКМ — один")
    }
    acceptIf { it.type == Material.DIAMOND_SWORD }
    loreLine("<gray>Предмет находится в меню")
    onChange { refresh(pos('B').first()) }
}
~~~

Нажатие на барьер переносит предмет с курсора в target. Барьер скрывается и восстанавливается после полного извлечения. Дополнительный лор показывается в меню. Игрок получает исходный предмет.

| Режим | Перенос через барьер |
| --- | --- |
| InputTransfer.CLICK | ЛКМ — стак, ПКМ — один предмет |
| InputTransfer.ALL | Всегда весь стак |
| InputTransfer.ONE | Всегда один предмет |

Режим задаётся через transfer(...). Из целевого слота предмет извлекается ЛКМ, ПКМ или shift-кликом. Связанные слоты защищены от хотбара, выбрасывания и drag. Shift-клик из инвентаря направляется в обычные поля.

## Фильтрация

~~~kotlin
acceptIf { player, item ->
    player.hasPermission("upgrade.use") &&
        item.type == Material.DIAMOND_SWORD &&
        item.amount == 1
}
~~~

Все объявленные acceptIf должны выполниться. Предикат получает копию предполагаемого предмета. Изменение этой копии сохраняет исходный предмет. Отклонённый перенос событий не создаёт.

## События

| Событие | Когда вызывается |
| --- | --- |
| onInsert | Добавление предмета или увеличение стака |
| onTake | Извлечение, уменьшение стака и takeInput |
| onChange | Любое фактическое изменение содержимого |

При замене порядок вызовов — onTake, onInsert, onChange. Обработчики выполняются отложенно на основном потоке.

| Поле change | Содержимое |
| --- | --- |
| slot | Целевой слот поля |
| cause | InputCause.PLAYER или InputCause.TAKE_INPUT |
| previous | Копия прежнего содержимого |
| current | Копия нового содержимого |
| item | Изменённое количество для onInsert и onTake |

Возврат при закрытии событий не создаёт. Ранее зафиксированное событие может прийти после закрытия. Для анализа изменения используйте previous и current. refresh и close относятся к исходной сессии обработчика.

## Чтение и извлечение

~~~kotlin
onClick {
    val preview = inputItem(target) ?: return@onClick
    player.sendMessage("Материал: " + preview.type.name)

    val taken = takeInput(target) ?: return@onClick
    if (!giveItem(taken)) {
        player.world.dropItem(player.location, taken).owner = player.uniqueId
    }
}
~~~

inputItem возвращает копию оригинала и оставляет предмет в поле. takeInput извлекает оригинал без временного лора. После извлечения за его выдачу или обработку отвечает ваш код.

При обычном закрытии AdrenalinGUI возвращает оставшиеся предметы. При нехватке места остаток выпадает рядом с игроком с установленным владельцем.
