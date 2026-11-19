package dev.aethereal.agui

import dev.aethereal.agui.menu.text.MenuText
import dev.aethereal.agui.menu.text.TextContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MenuTextTest {
    @Test fun staticTextNeverCallsPlaceholderResolver() {
        val text = MenuText("<green>Статический текст")
        val context = TextContext { error("Для статического текста парсер вызываться не должен") }
        assertSame(text.component, context.render(text))
        assertFalse(text.dynamic)
    }

    @Test fun separateOpenContextsDoNotSharePlayerValues() {
        val text = MenuText("<green>%player_name%")
        val alice = TextContext { it.replace("%player_name%", "Alice") }
        val bob = TextContext { it.replace("%player_name%", "Bob") }
        assertEquals(Component.text("Alice", NamedTextColor.GREEN), alice.render(text))
        assertEquals(Component.text("Bob", NamedTextColor.GREEN), bob.render(text))
        assertEquals(Component.text("Alice", NamedTextColor.GREEN), alice.render(text))
    }

    @Test fun identicalSourceIsResolvedOncePerOpen() {
        var calls = 0
        val context = TextContext { calls++; it.replace("%player_name%", "Alice") }
        val title = MenuText("<green>%player_name%")
        val name = MenuText("<green>%player_name%")
        assertSame(context.render(title), context.render(name))
        assertEquals(1, calls)
    }

    @Test fun reopeningGetsFreshValues() {
        val text = MenuText("<yellow>%vault_eco_balance%")
        val first = TextContext { it.replace("%vault_eco_balance%", "100") }
        val reopened = TextContext { it.replace("%vault_eco_balance%", "250") }
        assertEquals(Component.text("100", NamedTextColor.YELLOW), first.render(text))
        assertEquals(Component.text("250", NamedTextColor.YELLOW), reopened.render(text))
    }

    @Test fun unknownPlaceholdersAndMissingIntegrationPreserveSource() {
        val text = MenuText("<red>%unknown_value%")
        assertTrue(text.dynamic)
        assertSame(text.component, TextContext { it }.render(text))
    }

    @Test fun placeholderResultsAreParsedAsMiniMessage() {
        val text = MenuText("%custom_rank%")
        val context = TextContext { it.replace("%custom_rank%", "<gold>Admin</gold>") }
        assertEquals(Component.text("Admin", NamedTextColor.GOLD), context.render(text))
    }

    @Test fun percentagesWithoutCompletePlaceholderStayStatic() {
        assertFalse(MenuText("Скидка 25%").dynamic)
        assertFalse(MenuText("%player_name").dynamic)
        assertFalse(MenuText("100%%").dynamic)
        assertFalse(MenuText("%player\nname%").dynamic)
    }
}
