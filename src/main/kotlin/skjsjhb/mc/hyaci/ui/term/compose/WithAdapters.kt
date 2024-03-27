package skjsjhb.mc.hyaci.ui.term.compose

import skjsjhb.mc.hyaci.ui.term.ArgumentAdapter
import kotlin.reflect.KClass

/**
 * States that the specified adapters should be appended when this command registers.
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class WithAdapters(
    vararg val adapters: KClass<out ArgumentAdapter<Any>>
)
