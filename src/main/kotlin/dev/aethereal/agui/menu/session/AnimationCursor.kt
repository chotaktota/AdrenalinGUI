package dev.aethereal.agui.menu.session

internal class AnimationCursor<T>(private val frames: List<Pair<Long, T>>, private val period: Long?) {
    private var index = 0
    private var previous = 0L
    private var wrapped = false
    init {
        require(frames.isNotEmpty()) { "Анимация должна содержать шаги" }
        require(frames.first().first >= 0 && frames.zipWithNext().all { (a, b) -> a.first < b.first }) { "Тики анимации должны возрастать" }
        require(period == null || period > frames.last().first) { "Период должен быть больше последнего тика анимации" }
    }
    fun next(): Pair<Long, T>? {
        if (index == frames.size) {
            if (period == null) return null
            index = 0
            wrapped = true
        }
        val (tick, frame) = frames[index++]
        val delay = if (wrapped) checkNotNull(period) - previous + tick else tick - previous
        wrapped = false
        previous = tick
        return delay to frame
    }
}
