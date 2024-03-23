package skjsjhb.mc.hyaci.ui.term

import kotlin.reflect.KClass

annotation class WithAdapters(
    vararg val adapters: KClass<out ArgumentAdapter<Any>>
)
