package dev.aethereal.agui.engine.cache
internal class MenuCache<T : Any> {
    private var value: T? = null

    fun getOrBuild(build: () -> T): T = value ?: build().also { value = it }

    fun clear() { value = null }
}
