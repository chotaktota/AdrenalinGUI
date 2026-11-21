package dev.aethereal.agui.engine.cache

import dev.aethereal.agui.surface.item.GUIClick

internal class CachedNode(val item: CachedItem, val handler: (GUIClick.() -> Unit)?)
