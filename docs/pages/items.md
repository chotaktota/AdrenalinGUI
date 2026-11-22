# Предметы и метаданные

Материал, название, лор, зачарования и компоненты задаются внутри iconAt.

## Основные свойства

~~~kotlin
iconAt(pos('S')) {
    type(Material.DIAMOND_SWORD)
    amount(1)
    name("<red>Меч стража")
    loreLine("<gray>Предмет из меню сервера")
    enchant(Enchantment.SHARPNESS, 5)
    attribute(
        Attribute.ATTACK_DAMAGE,
        NamespacedKey(plugin, "menu_bonus"),
        2.0,
        slot = EquipmentSlotGroup.MAINHAND
    )
    unbreakable()
    glow()
    allFlags()
}
~~~

| Метод | Назначение |
| --- | --- |
| type(Material) | Материал иконки |
| amount(Int) | Количество с учётом допустимого размера стака |
| name(String) | Название с форматированием |
| loreLine(String) | Добавить строку лора |
| glow() | Визуальное свечение |
| allFlags(), flags(...) | Скрыть стандартные описания свойств |
| enchant(...) | Зачарование предмета или зачарованной книги |
| attribute(...) | Модификатор атрибута с уникальным ключом |

## Зелья

~~~kotlin
iconAt(pos('P')) {
    type(Material.POTION)
    name("<red>Зелье")
    potionType(PotionType.HEALING)
    potionColor(Color.fromRGB(190, 42, 48))
    potionEffect(PotionEffect(PotionEffectType.SPEED, 200, 1))
}
~~~

Для цвета и эффектов требуется материал, поддерживающий PotionMeta.

## Модели и экипировка

~~~kotlin
iconAt(pos('H')) {
    type(Material.CARVED_PUMPKIN)
    modelData(1001)
    itemModel(NamespacedKey(plugin, "menu_hat"))
    equippable(EquipmentSlot.HEAD)
}
~~~

~~~kotlin
modelData {
    floats = listOf(1001f)
    strings = listOf("hat")
    colors = listOf(Color.RED)
}
~~~

modelData(Int) и modelData(Float) задают числовую часть компонента CustomModelData. Для модели предмета используйте itemModel, для слота экипировки — equippable.

## Собственные данные

~~~kotlin
nbt(
    NamespacedKey(plugin, "category"),
    PersistentDataType.STRING,
    "hats"
)
~~~

nbt работает с PersistentDataContainer через NamespacedKey и PersistentDataType. Передайте null для удаления собственного ключа.

Для стандартных свойств ItemMeta доступны meta и типизированная перегрузка meta(Class).

~~~kotlin
meta {
    isUnbreakable = true
}
~~~

## Головы

Для PLAYER_HEAD доступен headTexture(value). Передайте текстуру из своей конфигурации — URL textures.minecraft.net, хеш или Base64 с JSON скина.

~~~kotlin
iconAt(pos('H')) {
    type(Material.PLAYER_HEAD)
    headTexture(textureFromConfig)
    name("<white>Профиль")
}
~~~

Здесь textureFromConfig — строка текстуры, загруженная вашим плагином. Метод создаёт профиль со ссылкой на скин и не выполняет HTTP-запрос при построении предмета.
