package skjsjhb.mc.hyaci.util

/**
 * Clamps a value to a range.
 */
fun <T : Comparable<T>> T.clamp(r: ClosedRange<T>): T =
    if (this < r.start) r.start else if (this > r.endInclusive) r.endInclusive else this

/**
 * Clamps a value to a minimum.
 */
fun <T : Comparable<T>> T.clampMin(min: T): T = if (this < min) min else this

/**
 * Clamps a value to a maximum.
 */
fun <T : Comparable<T>> T.clampMax(max: T): T = if (this > max) max else this