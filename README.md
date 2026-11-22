# AdrenalinGUI

Kotlin menus for Minecraft plugins. Define the slots, style the items, and put your plugin logic directly into button actions.

[Русский](README.ru.md) · [Documentation](https://chotaktota.github.io/AdrenalinGUI/) · [Download](https://github.com/chotaktota/AdrenalinGUI/releases/latest)

![AdrenalinGUI](docs/assets/wordmark.png)

## 🧩 What you can build

- **A simple server menu** — arrange items with a nine-character pattern and attach Kotlin actions.
- **Help with categories** — keep the main page, economy, mechanics, and server information in one `MultiGUISurface`.
- **Player-specific items** — use MiniMessage, legacy colors, PlaceholderAPI, and `dynamic` callbacks.
- **Item-processing menus** — accept player items into selected slots, filter them, and react to insertions and withdrawals.
- **Buttons with conditions** — show an unavailable state and its reason, check access again on click, and apply `cooldown`.
- **Tick-based animation** — set, change, hide, and show items at declared ticks.
- **Economy and permissions** — call Vault and LuckPerms actions directly, including balance, transactions, permissions, and groups.
- **Typed chat input** — request a number or text with validation, success, and failure handlers.

Templates remove repeated item setup. Item builders cover enchantments, attributes, potions, heads, custom models, equipment components, and persistent data. Sound actions provide feedback for success, rejection, and navigation.

Each player gets a separate inventory. The shared cache stores menu definitions and item prototypes. Refresh the affected slots explicitly when your data changes.

## 📦 Install

| Component | Requirement |
| --- | --- |
| Java | 21 |
| Server | Paper 1.21.11 or an API-compatible fork |
| Kotlin compiler used by the project | 2.5.0-Beta2 |
| Optional integrations | PlaceholderAPI and expansions, Vault with an economy provider, LuckPerms |

1. Download the JAR from [Releases](https://github.com/chotaktota/AdrenalinGUI/releases/latest).
2. Put it in the server's `plugins/` directory and restart.
3. Put the same JAR in your plugin project's `libs/` directory and add the API dependency.

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files("libs/AdrenalinGUI-2.0.1.jar"))
}
```

Declare the server dependency in your plugin's `plugin.yml`.

```yaml
depend: [AdrenalinGUI]
```

[Full Gradle setup and paper-plugin.yml](https://chotaktota.github.io/AdrenalinGUI/#/installation)

## ⚡ Your first menu

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
            title("<red>Server menu")

            iconAt(pos('X')) {
                type(Material.ENDER_EYE)
                name("<white>Close")
                loreLine("<gray>Click to close this menu")
                allFlags()
                onClick { close() }
            }
        }
    }
}
```

Create and register one instance in your plugin's `onEnable`.

```kotlin
val engine = JavaPlugin.getPlugin(AdrenalinGUI::class.java).engine
engine.menus.register("main", MainMenu(this))
```

Open it from your command or listener on the server's main thread.

```kotlin
engine.open("main", player)
```

[Complete plugin example](https://chotaktota.github.io/AdrenalinGUI/#/quick-start) · [Menu examples](https://chotaktota.github.io/AdrenalinGUI/#/examples)

## 📚 Documentation

| You need | Start here |
| --- | --- |
| Layouts and repeated icons | [Patterns](https://chotaktota.github.io/AdrenalinGUI/#/layout) and [templates](https://chotaktota.github.io/AdrenalinGUI/#/templates) |
| Categories in one menu | [Pages and back navigation](https://chotaktota.github.io/AdrenalinGUI/#/pages) |
| Item input, filters, and events | [Player item fields](https://chotaktota.github.io/AdrenalinGUI/#/fields) |
| Disabled buttons and click limits | [Conditions and cooldown](https://chotaktota.github.io/AdrenalinGUI/#/conditions) |
| Fresh values with shared caching | [Dynamic items and refresh](https://chotaktota.github.io/AdrenalinGUI/#/dynamic) |
| Chat, money, groups, and sound | [Actions](https://chotaktota.github.io/AdrenalinGUI/#/actions) and [integrations](https://chotaktota.github.io/AdrenalinGUI/#/economy) |
| Upgrade an existing plugin | [API 2.x migration](https://chotaktota.github.io/AdrenalinGUI/#/migration) |

Use `/guis info` to inspect registered menus and their owning plugins. The command requires `adrenalingui.admin`.

## 📊 Control benchmark

The November 22 control run used AdrenalinGUI 2.0.1, eight bots, and identical 54-slot menus. Median server-side open time was **0.0735 ms** for static items and **0.1118 ms** with placeholders. The click-action measurement includes next-tick dispatch.

Results vary by scenario. The documentation includes the BMenu and AbstractMenus comparison, p95 values, methodology, and raw samples.

[Results and methodology](https://chotaktota.github.io/AdrenalinGUI/#/benchmarks)

## Build

```bash
./gradlew clean build
```

On Windows, use `.\gradlew.bat clean build`. The Gradle Wrapper is included. Runnable menu examples are in [src/test](src/test/kotlin/dev/aethereal/agui/example).
