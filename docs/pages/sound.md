# Звуки действий

sound воспроизводит звук только игроку текущего обработчика. Доступны стандартные звуки и строковые ключи.

## Стандартный звук

~~~kotlin
onClick {
    sound(Sound.UI_BUTTON_CLICK)
    sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume = 0.7f, pitch = 1.2f)
}
~~~

## Ресурспак и категория

~~~kotlin
onClick {
    sound("myserver:menu.success", volume = 0.5f, pitch = 1f)
    sound("minecraft:ui.button.click", category = SoundCategory.MASTER)
}
~~~

Звук ресурспака должен присутствовать у клиента. Громкость и высота — конечные неотрицательные Float. Для строк поддерживаются ключи namespace:name и имена стандартных Sound.

## Звуки событий и отказа

~~~kotlin
onInsert { sound(Sound.BLOCK_ANVIL_PLACE) }
onTake { sound(Sound.ENTITY_ITEM_PICKUP) }
~~~

Внутри иконки можно добавить звук недоступного действия.

~~~kotlin
onDenied {
    sound(Sound.ENTITY_VILLAGER_NO)
}
~~~
