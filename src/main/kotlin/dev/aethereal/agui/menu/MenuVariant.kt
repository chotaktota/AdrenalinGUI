package dev.aethereal.agui.menu

enum class MenuVariant(val isBedrock: Boolean, val hasResourcePack: Boolean) {
    JAVA_RP(false, true),
    JAVA_NRP(false, false),
    BEDROCK_RP(true, true),
    BEDROCK_NRP(true, false)
}
