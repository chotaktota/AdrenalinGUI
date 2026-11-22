# Условия и задержка кликов

Кнопка может иметь проверки видимости и доступности, причину отказа и ограничение частоты кликов.

## enabledIf

~~~kotlin
iconAt(pos('B')) {
    type(Material.ANVIL)
    name("<green>Проверить предмет")

    enabledIf("<red>Нужно право upgrade.use") {
        it.hasPermission("upgrade.use")
    }

    disabled { reason ->
        name("<gray>Проверить предмет")
        lore(reason)
        glow(false)
    }

    onDenied { sound(Sound.ENTITY_VILLAGER_NO) }
    onClick { sound(Sound.BLOCK_ANVIL_USE) }
}
~~~

Кнопка остаётся видимой. При false применяется disabled, а действие onClick блокируется. Без своего onDenied причина отправляется игроку с форматированием. Пользовательский onDenied получает строку причины.

Условие проверяется для конкретного игрока после открытия окна, при refresh и заново перед выполнением клика. Кэш хранит описание условия, а его результат вычисляется по актуальным данным.

## Несколько условий

~~~kotlin
enabledIf("<red>Положите алмазный меч") { player ->
    player.openInventory.topInventory.getItem(inputSlot)?.type == Material.DIAMOND_SWORD
}

enabledIf("<red>Нужно право upgrade.use") {
    it.hasPermission("upgrade.use")
}
~~~

Требуется выполнение всех проверок. Причину определяет первая невыполненная проверка в порядке объявления.

## Видимость

~~~kotlin
iconAt(pos('V')) {
    type(Material.DIAMOND)
    requirePermission("shop.vip")
    visibleIf { player -> player.level >= 10 }
    name("<aqua>Раздел VIP")
}
~~~

visibleIf и requirePermission управляют присутствием иконки в слоте. Все проверки должны выполниться. При изменении условий вызовите refresh. Скрытая иконка не выполняет обработчик клика.

## cooldown

~~~kotlin
iconAt(pos('B')) {
    type(Material.EMERALD)
    cooldown(10)
    onClick {
        player.sendMessage("Действие выполнено")
    }
}
~~~

Задержка измеряется в серверных тиках и относится к игроку и объявленной иконке в текущей сессии. Позиции одной иконки делят задержку. Повторное открытие создаёт новую сессию. Таймеры отсчёта не создаются.

## Обновление оформления

В onChange поля вызовите refresh(buttonSlot). Иконка пересчитает оформление по актуальному содержимому. Если баланс или параметры изменились другим кодом, этот код также должен запросить refresh.
