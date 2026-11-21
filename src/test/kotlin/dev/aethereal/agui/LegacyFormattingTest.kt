package dev.aethereal.agui

import dev.aethereal.agui.text.MenuText
import dev.aethereal.agui.text.TextContext
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyFormattingTest {
    private val legacy = LegacyComponentSerializer.builder().character('§')
        .hexColors().useUnusualXRepeatedCharacterHexFormat().build()

    private fun render(text: String) = legacy.serialize(MenuText(text).component)
    private fun expected(text: String) = legacy.serialize(MiniMessage.miniMessage().deserialize(text))

    @Test fun bothLegacyMarkersAndUppercaseCodesAreSupported() {
        assertEquals("§aЗелёный", render("&aЗелёный"))
        assertEquals("§cКрасный", render("§cКрасный"))
        assertEquals("§bГолубой", render("&BГолубой"))
    }

    @Test fun legacyColorsResetPreviousDecorations() {
        assertEquals("§lЖирный §aОбычный", render("&lЖирный &aОбычный"))
        assertEquals("§lЖирный §fОбычный", render("&lЖирный &rОбычный"))
    }

    @Test fun allLegacyDecorationsAreSupported() {
        for ((code, tag) in mapOf('k' to "obfuscated", 'l' to "bold", 'm' to "strikethrough",
            'n' to "underlined", 'o' to "italic")) {
            assertEquals(expected("<$tag>Текст"), render("&$code" + "Текст"))
        }
    }

    @Test fun legacyHexFormatsHaveTheSameColorAsMiniMessage() {
        val color = expected("<#a1b2c3>Текст")
        assertEquals(color, render("&#a1b2c3Текст"))
        assertEquals(color, render("§#A1B2C3Текст"))
        assertEquals(color, render("&x&A&1&B&2&C&3Текст"))
        assertEquals(color, render("§x§a§1§b§2§c§3Текст"))
    }

    @Test fun miniMessageAndLegacyCanBeMixed() {
        assertEquals("§aПривет §lмир", render("&aПривет <bold>мир</bold>"))
        assertEquals("§aЗелёный §cКрасный", render("<green>Зелёный &cКрасный</green>"))
    }

    @Test fun legacyCodesDoNotCaptureMiniMessageClosingTags() {
        assertEquals(expected("<bold>Жирный </bold><green>Обычный</green> конец"),
            render("<bold>Жирный &aОбычный</bold> конец"))
        assertEquals(expected("<green>Раз <bold>два</bold></green> три"),
            render("<green>Раз &lдва</green> три"))
    }

    @Test fun legacyValuesFromPlaceholdersAreFormatted() {
        val text = MenuText("<gray>Ранг: %rank%")
        val context = TextContext { it.replace("%rank%", "&6&lАдмин") }
        assertEquals("§7Ранг: §6§lАдмин", legacy.serialize(context.render(text)))
    }

    @Test fun tagAttributesAndEscapedMiniMessageRemainIntact() {
        val input = "<click:open_url:'https://example.org/?a=1&b=2'>Ссылка</click>"
        assertEquals(MiniMessage.miniMessage().deserialize(input), MenuText(input).component)
        assertEquals("<red>Текст", render("\\<red>Текст"))
    }

    @Test fun invalidCodesAndIncompleteHexRemainLiteral() {
        assertEquals("AT&T &z &#12345", render("AT&T &z &#12345"))
    }
}
