package dev.aethereal.agui.engine.session

import dev.aethereal.agui.engine.cache.CachedMenu
import dev.aethereal.agui.surface.GUISurface

internal class MenuDestination(
    val surface: GUISurface,
    val template: CachedMenu,
    val onOpen: () -> Unit,
    val onClose: () -> Unit,
    val page: String?,
    val onNavigate: ((String) -> Unit)?
)
