package dev.aethereal.agui.engine.session
internal class SessionLifecycle(private val onOpen: () -> Unit, private val onClose: () -> Unit) {
    private var opened = false
    private var closed = false

    fun open() {
        if (opened || closed) return
        opened = true
        onOpen()
    }

    fun close() {
        if (closed) return
        closed = true
        if (opened) onClose()
    }
}
