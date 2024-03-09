package skjsjhb.mc.hyaci.util

/**
 * Generic pair structure.
 */
class Pair<T>(private var first: T, private var other: T) : AbstractList<T>() {
    override val size: Int = 2

    override fun get(index: Int): T =
        when (index) {
            0 -> first
            1 -> other
            else -> throw IndexOutOfBoundsException("Illegal index for pair: $index")
        }

    operator fun component1(): T = first

    operator fun component2(): T = other
}