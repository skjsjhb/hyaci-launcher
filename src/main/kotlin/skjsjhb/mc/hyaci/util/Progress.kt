package skjsjhb.mc.hyaci.util

/**
 * A progressed object can provide progress information.
 */
interface Progressed {
    /**
     * Sets the progress handler.
     */
    fun setProgressHandler(handler: (status: String, progress: Double) -> Unit)
}

/**
 * Wraps an [Iterable] object and executes corresponding progress handler during iteration.
 */
fun <T> Iterable<T>.withProgress(handler: (status: String, progress: Double) -> Unit): Iterable<T> =
    this.let { parent ->
        object : Iterable<T> {
            private val backedIterator = parent.iterator()
            private val count = parent.count()
            override fun iterator(): Iterator<T> =
                object : Iterator<T> by backedIterator {
                    private var index = 0
                    override fun next(): T {
                        index++
                        handler("", index.toDouble() / count)
                        return backedIterator.next()
                    }
                }
        }
    }

