package dev.aethereal.agui.menu.cache

import dev.aethereal.agui.surface.GUIClick

internal class CachedNode(val item: CachedItem, val handler: (GUIClick.() -> Unit)?)
