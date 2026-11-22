# AdrenalinGUI

Библиотека меню для Minecraft-плагинов на Kotlin. Задайте расположение слотов, оформите предметы и вызывайте логику своего плагина прямо из кнопок.

[English](README.md) · [Документация](https://chotaktota.github.io/AdrenalinGUI/) · [Скачать](https://github.com/chotaktota/AdrenalinGUI/releases/latest)

![AdrenalinGUI](docs/assets/wordmark.png)

## 🧩 Что можно сделать

- **Обычное меню сервера** — расположите предметы по шаблону и добавьте действия на Kotlin.
- **Помощь с категориями** — главная страница, экономика, механики и информация о сервере помещаются в один `MultiGUISurface`.
- **Предметы с данными игрока** — используйте MiniMessage, legacy, PlaceholderAPI и обработчик `dynamic`.
- **Меню обработки предметов** — принимайте предметы в выбранные слоты, фильтруйте их и реагируйте на вставку и извлечение.
- **Кнопки с условиями** — покажите причину недоступности, повторно проверьте доступ при клике и задайте `cooldown`.
- **Анимации по тикам** — устанавливайте, изменяйте, скрывайте и показывайте предметы на нужных шагах.
- **Экономику и права** — вызывайте действия Vault и LuckPerms, включая баланс, транзакции, выдачу прав и групп.
- **Ввод через чат** — запросите число или текст с проверкой, обработчиками успеха и ошибки.

Шаблоны иконок сокращают повторяющееся оформление. Билдеры поддерживают чары, атрибуты, зелья, головы, модели, экипировку и собственные данные. Звуки помогают обозначить успех, отказ и переход.

Каждый игрок получает отдельный инвентарь. Общий кэш хранит описания меню и прототипы предметов. После изменения данных явно обновляйте нужные слоты через `refresh`.

## 📦 Установка

| Компонент | Требование |
| --- | --- |
| Java | 21 |
| Сервер | Paper 1.21.11 или совместимый форк |
| Компилятор проекта | Kotlin 2.5.0-Beta2 |
| Дополнительные интеграции | PlaceholderAPI с расширениями, Vault с экономическим провайдером, LuckPerms |

1. Скачайте JAR из [Releases](https://github.com/chotaktota/AdrenalinGUI/releases/latest).
2. Положите его в `plugins/` сервера и перезапустите сервер.
3. Добавьте тот же JAR в `libs/` своего плагина и подключите API.

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files("libs/AdrenalinGUI-2.0.1.jar"))
}
```

Укажите зависимость в `plugin.yml` своего плагина.

```yaml
depend: [AdrenalinGUI]
```

[Полная настройка Gradle и paper-plugin.yml](https://chotaktota.github.io/AdrenalinGUI/#/installation)

## ⚡ Первое меню

```kotlin
import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class MainMenu(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ....X....
            .........
        """.trimIndent())) {
            title("<red>Меню сервера")

            iconAt(pos('X')) {
                type(Material.ENDER_EYE)
                name("<white>Закрыть")
                loreLine("<gray>Нажмите, чтобы закрыть меню")
                allFlags()
                onClick { close() }
            }
        }
    }
}
```

Создайте и зарегистрируйте один экземпляр в `onEnable` своего плагина.

```kotlin
val engine = JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine
engine.menus.register("main", MainMenu(this))
```

Открывайте его из команды или слушателя на основном потоке сервера.

```kotlin
engine.open("main", player)
```

[Полный пример плагина](https://chotaktota.github.io/AdrenalinGUI/#/quick-start) · [Примеры меню](https://chotaktota.github.io/AdrenalinGUI/#/examples)

## 📚 Документация

| Задача | Раздел |
| --- | --- |
| Разложить предметы и убрать повторы | [Шаблоны слотов](https://chotaktota.github.io/AdrenalinGUI/#/layout) и [иконок](https://chotaktota.github.io/AdrenalinGUI/#/templates) |
| Собрать категории в одном классе | [Страницы и возврат](https://chotaktota.github.io/AdrenalinGUI/#/pages) |
| Принимать предметы игрока | [Поля, фильтры и события](https://chotaktota.github.io/AdrenalinGUI/#/fields) |
| Настроить доступность кнопки | [Условия и cooldown](https://chotaktota.github.io/AdrenalinGUI/#/conditions) |
| Менять значения с учётом кэша | [Динамические иконки и refresh](https://chotaktota.github.io/AdrenalinGUI/#/dynamic) |
| Работать с чатом, деньгами, группами и звуком | [Действия](https://chotaktota.github.io/AdrenalinGUI/#/actions) и [интеграции](https://chotaktota.github.io/AdrenalinGUI/#/economy) |
| Обновить старый плагин | [Переход на API 2.x](https://chotaktota.github.io/AdrenalinGUI/#/migration) |

Команда `/guis info` показывает зарегистрированные меню и плагины-владельцы. Требуется право `adrenalingui.admin`.

## 📊 Контрольные замеры

Прогон 22 ноября выполнен на AdrenalinGUI 2.0.1 с восемью ботами и одинаковыми меню на 54 слота. Медиана серверного открытия — **0,0735 мс** для статичных предметов и **0,1118 мс** с плейсхолдерами. Замер действия включает отложенный запуск на следующем тике.

Результат зависит от сценария. В документации приведены сравнение с BMenu и AbstractMenus, p95, условия стенда и исходные образцы.

[Результаты и методика](https://chotaktota.github.io/AdrenalinGUI/#/benchmarks)

## Сборка

```bash
./gradlew clean build
```

В Windows используйте `.\gradlew.bat clean build`. Gradle Wrapper включён в проект. Полные классы меню находятся в [src/test](src/test/kotlin/dev/aethereal/agui/example).
