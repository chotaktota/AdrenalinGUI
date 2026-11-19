<div align="center">

<img src="docs/assets/banner.png" alt="AdrenalinGUI — меню Minecraft на Kotlin" width="100%">

<p><strong>Kotlin DSL для инвентарных меню в ваших Minecraft-плагинах.</strong></p>

<p>
<img alt="Minecraft 1.21.11" src="https://img.shields.io/badge/Minecraft-1.21.11-55AA77?style=flat-square">
<img alt="Java 21" src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square">
<img alt="Kotlin 2.5.0 Beta2" src="https://img.shields.io/badge/Kotlin-2.5.0--Beta2-7F52FF?style=flat-square">
<img alt="Ранняя разработка" src="https://img.shields.io/badge/status-early_development-55AAFF?style=flat-square">
</p>

[English](README.md) · **Русский**

[Начало работы](#первое-меню) · [Страницы](#одно-меню-несколько-страниц) · [Сравнение](#bmenu-и-abstractmenus) · [Примеры](#примеры) · [Требования](#требования)

</div>

> Обложка — иллюстрация, сгенерированная ИИ, а не скриншот из игры. Проект находится в ранней разработке: API может меняться.

## Меню рядом с логикой вашего плагина

Задайте расположение символами, добавьте предметы и обработайте клики на Kotlin. Для обычного окна есть `SingleGUISurface`, для справки с категориями — `MultiGUISurface`: все разделы остаются в одном классе.

- **Понятный шаблон** — девять символов в строке, повторяющиеся символы заполняют несколько слотов.
- **MiniMessage везде** — теги, HEX и градиенты в заголовках, названиях и описаниях.
- **Именованные страницы** — `page("economy")` и `openPage("economy")`, отдельный кэш каждой страницы.
- **События жизненного цикла** — `onOpen` и `onClose` для каждого успешно открытого окна.
- **Кэш прототипов** — шаблон и метаданные предметов собираются один раз; игроки получают отдельные инвентари и копии предметов.
- **Вызовы вашего API** — обработчики кнопок могут обращаться к сервисам плагина напрямую на Kotlin.

AdrenalinGUI устанавливается отдельным плагином. Публичная сборка использует Paper/Adventure напрямую, Kotlin runtime включён в JAR.

## Требования

| Компонент | Требование |
| --- | --- |
| Java | 21 |
| Сервер | Paper 1.21.11 или совместимый по API форк; целевое ядро разработки — UniverseSpigot |
| Компилятор Kotlin | В проекте 2.5.0-Beta2; используйте эту версию или совместимый компилятор |
| Сборка | Gradle Wrapper 9.6.0 уже включён |
| Дополнительные плагины | Не требуются |
| Folia | Текущий планировщик основного потока не поддерживает Folia |

Сборка и модульные тесты проверяются локально. Автоматических тестов на запущенном сервере и нагрузочного теста с игроками пока нет. Совместимость с версиями вне целевой не заявляется.

## Сборка и установка

```bash
git clone https://github.com/chotaktota/AdrenalinGUI.git
cd AdrenalinGUI
./gradlew clean build publishToMavenLocal
```

На Windows:

```powershell
.\gradlew.bat clean build publishToMavenLocal
```

Положите `build/libs/AdrenalinGUI-1.0-SNAPSHOT.jar` в папку `plugins/` сервера и перезапустите сервер.

В `build.gradle.kts` своего плагина:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    compileOnly("dev.aethereal:AdrenalinGUI:1.0-SNAPSHOT")
}
```

Координаты относятся к артефакту, опубликованному предыдущей командой в локальный Maven. Отдельный Maven-репозиторий и релиз через JitPack пока не настроены.

В `plugin.yml` своего плагина:

```yaml
depend: [AdrenalinGUI]
```

Если используется `paper-plugin.yml`:

```yaml
dependencies:
  server:
    AdrenalinGUI:
      load: BEFORE
      required: true
      join-classpath: true
```

## Первое меню

```kotlin
import dev.aethereal.agui.surface.SingleGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class MainGUI(plugin: JavaPlugin) : SingleGUISurface(3, plugin) {
    override fun onUpdateNodes() {
        with(newPattern("""
            .........
            ....X....
            .........
        """.trimIndent())) {
            title("<gradient:#55FFAA:#55AAFF>Главное меню</gradient>")
            iconAt(pos('X')) {
                type(Material.ENDER_EYE)
                name("<white>Око Эндера")
                loreLine("<gray>Ваше меню. Ваш код на Kotlin.")
                loreLine("<green>Нажмите, чтобы закрыть")
                glow()
                allFlags()
                onClick { close() }
            }
        }
    }
}
```

Создайте экземпляр один раз в своём плагине и переиспользуйте:

```kotlin
val menu = MainGUI(this)
menu.open(player)
```

В шаблоне должно быть указанное число строк, в каждой — ровно девять символов. Точка обозначает пустой слот. `pos('X')` возвращает все слоты с символом `X`; `iconAt(13)` задаёт слот по индексу с нуля.

## Одно меню, несколько страниц

```kotlin
import dev.aethereal.agui.surface.MultiGUISurface
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class GuideGUI(plugin: JavaPlugin) : MultiGUISurface(1, plugin) {
    override fun onUpdateNodes() {
        page("main") {
            with(newPattern("....E....")) {
                title("<gold>Помощь")
                iconAt(pos('E')) {
                    type(Material.GOLD_INGOT)
                    name("<yellow>Экономика")
                    onClick { openPage("economy") }
                }
            }
        }
        page("economy") {
            with(newPattern("....B....")) {
                title("<yellow>Экономика")
                iconAt(pos('B')) {
                    type(Material.ARROW)
                    name("<yellow>Назад")
                    onClick { openPage("main") }
                }
            }
        }
    }
}
```

`menu.open(player)` открывает `main`, а `menu.open(player, "economy")` — конкретный раздел. Начальную страницу можно изменить третьим аргументом конструктора. Размер отдельной страницы: `page("economy", rows = 6)`.

В полном примере [HelpGUI](src/test/kotlin/dev/aethereal/agui/example/HelpGUI.kt) есть четыре страницы: главная, экономика, механики и информация о сервере.

## Регистрация и открытие по имени

```kotlin
import dev.aethereal.agui.AdrenalinGUI
import org.bukkit.plugin.java.JavaPlugin

val engine = JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine
engine.menus.register("help", HelpGUI(this))
engine.open("help", player)
```

Регистрация необязательна: можно вызывать `open` сохранённого экземпляра напрямую. `engine.menus.find("help")` возвращает меню или null, `unregister("help")` удаляет имя и очищает кэш. Уже открытые окна продолжают работать до закрытия.

[HelpExamplePlugin.kt](src/test/kotlin/dev/aethereal/agui/example/HelpExamplePlugin.kt) показывает подключение команды. Для него нужен такой дескриптор:

```yaml
name: HelpExample
version: '1.0'
main: dev.aethereal.agui.example.HelpExamplePlugin
api-version: '1.21.11'
depend: [AdrenalinGUI]
commands:
  helpui:
    description: Открыть справку сервера
```

Классы примеров компилируются при тестировании и не входят в JAR фреймворка.

## События, клики и кэш

![Из кэшированного шаблона создаются независимые инвентари игроков](docs/assets/cache-flow.svg)

В `SingleGUISurface` переопределяются `onOpen(player: Player)` и `onClose(player: Player)`. В `MultiGUISurface` — `onOpen(player: Player, page: String)` и `onClose(player: Player, page: String)`.

При переходе сначала вызывается закрытие старой страницы, затем открытие новой — если она успешно открылась. Отменённое открытие не вызывает обработчики новой страницы. Если Bukkit уже закрыл предыдущее окно, а другой плагин отменил новое открытие, предыдущее окно не восстанавливается.

| Действие | Поведение |
| --- | --- |
| Первое открытие | Собираются шаблон, текст и прототипы предметов |
| Повторное открытие | В отдельный инвентарь копируются готовые прототипы |
| `menu.invalidateCache()` | Пересборка при следующем открытии; текущие окна сохраняют содержимое |
| `multi.invalidateCache("economy")` | Сбрасывается только указанная страница |
| Отключение плагина-владельца | Закрываются сессии, удаляются регистрации и очищаются учтённые кэши |

Переиспользуйте экземпляры меню. Новый объект на каждый клик означает новый кэш. Кэшированный шаблон общий для зрителей: не захватывайте в нём персональные данные конкретного игрока.

В `onClick` доступны `player`, `slot`, `click`, `isLeftClick`, `isRightClick`, `isShiftClick`, а у многостраничного меню ещё и `page`. Обработчик запускается на следующем тике основного потока; переход в другое меню из кнопки безопасен. Дополнительные клики до запуска объединяются, а закрытие окна отменяет отложенное действие.

Перемещение предметов и drag блокируются в обоих инвентарях. Операции меню выполняются на основном потоке. Если другое меню нужно открыть из `onClose`, запланируйте открытие на следующий тик.

Периодических задач обновления и автоматического перебора игроков нет. Это описание реализации, а не утверждение о превосходстве по скорости.

## BMenu и AbstractMenus

Основные подходы отличаются: AdrenalinGUI задаёт меню классами Kotlin, BMenu — YAML, AbstractMenus — HOCON. У обоих альтернативных проектов есть API расширения; наличие Kotlin-обработчиков здесь не означает отсутствие API у них.

| Возможность / подход | AdrenalinGUI сейчас | BMenu | AbstractMenus |
| --- | --- | --- | --- |
| Основное описание меню | Kotlin-классы | YAML-файлы | HOCON-файлы |
| Вызов сервисов своего плагина | Напрямую из Kotlin | Действия конфига и API расширения | Действия конфига и API расширения |
| Справка с категориями | Именованные страницы в одном классе | Переходы между меню | Переходы между меню |
| Цветной текст | MiniMessage | MiniMessage и legacy | MiniMessage и legacy |
| Готовые анимации и обновления | Не реализованы | Описаны в документации | Описаны в документации |
| Встроенная работа с плейсхолдерами | Не реализована | PlaceholderAPI | PlaceholderAPI |
| Изменение меню без пересборки плагина | Не реализовано | Перезагрузка конфигов | Перезагрузка конфигов |
| Готовые действия экономики и условия | Свои Kotlin-обработчики | Действия и условия конфига | Правила и действия конфига |

Преимущество этого подхода — контроль над Kotlin-кодом плагина и явным сбросом кэша. Обратная сторона — меньше готовых возможностей для настройки сервера. Наличие страниц или кэша не заявляется как уникальное; сравнительных замеров производительности нет.

Одна задача — кнопка закрытия — в трёх вариантах:

**BMenu, YAML** — пример по [официальному руководству](https://wiki.bdev.space/bmenu/start):

```yaml
id: guide:home
size: 9
title: '<gold>Guide'
items:
  exit:
    slot: 4
    material: barrier
    name: '<red>Close'
    on_click:
      - close: ''
```

**AbstractMenus, HOCON** — пример по [официальному руководству](https://abstractmenus.github.io/docs/en/start/how-to/):

```hocon
title: "Guide"
size: 1
items: [
  {
    slot: 4
    material: BARRIER
    name: "Close"
    click { closeMenu: true }
  }
]
```

**AdrenalinGUI, внутри блока шаблона:**

```kotlin
iconAt(4) {
    type(Material.BARRIER)
    name("<red>Закрыть")
    onClick { close() }
}
```

Примеры сторонних плагинов иллюстрируют их синтаксис, не являются конфигами AdrenalinGUI и не запускались в тестах этого проекта.

Источники проверены 19 ноября 2026 года: [справочник BMenu](https://wiki.bdev.space/bmenu/reference), [репозиторий и API BMenu](https://github.com/By1337/BMenu), [обзор AbstractMenus](https://abstractmenus.github.io/docs/en/), [репозиторий AbstractMenus](https://github.com/AbstractMenus/minecraft-plugin).

## Примеры

| Пример | Что показывает |
| --- | --- |
| [QuickStartGUI](src/test/kotlin/dev/aethereal/agui/example/QuickStartGUI.kt) | Простое меню по шаблону |
| [HelpGUI](src/test/kotlin/dev/aethereal/agui/example/HelpGUI.kt) | Четыре страницы и возврат к категориям |
| [HelpExamplePlugin](src/test/kotlin/dev/aethereal/agui/example/HelpExamplePlugin.kt) | Регистрацию и команду игрока |
| [PreviewGUI](src/test/kotlin/dev/aethereal/agui/example/PreviewGUI.kt) | Большой шаблон на шесть строк |

## Структура

```text
menu/
  MenuEngine.kt
  MenuRegistry.kt
  MenuVariant.kt
  cache/
  session/
  page/
    MenuPage.kt
    PageManager.kt
    registry/
surface/
  SingleGUISurface.kt
  MultiGUISurface.kt
  pattern/
  icon/
```

## Что реализовано, а что дальше

Сейчас работают статические инвентарные меню Java, именованные страницы, MiniMessage, обработчики кликов, события открытия/закрытия и ручной сброс кэша.

Автоматическое разбиение списков по страницам, анимации, периодические обновления, интеграции плейсхолдеров и экономики, загрузчики конфигов, нативные Bedrock-формы и определение ресурспака/клиента пока не реализованы. Значения `JAVA_RP`, `JAVA_NRP`, `BEDROCK_RP`, `BEDROCK_NRP` в `MenuVariant` зарезервированы под будущее расширение.

`./gradlew test` запускает модульные тесты кэша, шаблонов, реестра страниц и жизненного цикла. Примеры также компилируются; поведение на живом сервере требует отдельной проверки.

## Обратная связь

Создайте [issue](https://github.com/chotaktota/AdrenalinGUI/issues), укажите Java, версию сервера, минимальный класс меню и шаги воспроизведения.

[English documentation →](README.md)
