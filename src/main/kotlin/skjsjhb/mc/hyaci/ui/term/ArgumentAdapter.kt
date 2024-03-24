package skjsjhb.mc.hyaci.ui.term

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor

/**
 * Reads and converts a string into the given argument type.
 */
interface ArgumentAdapter<out T> {
    /**
     * Runs the conversion.
     */
    fun get(src: String): T

    /**
     * Gets the type that this adapter will convert to.
     */
    fun target(): KType

    companion object {
        private val adapters: MutableMap<KType, ArgumentAdapter<*>> = HashMap()

        init {
            addAdapter(StringAdapter())
            addAdapter(BooleanAdapter())
        }

        /**
         * Instantiate and add an adapter class.
         */
        fun addAdapterClass(adapterClazz: KClass<out ArgumentAdapter<Any>>) {
            adapterClazz.primaryConstructor?.call()?.let { addAdapter(it) }
        }

        /**
         * Add an adapter object.
         */
        fun addAdapter(adapter: ArgumentAdapter<Any>) {
            adapters[adapter.target()] = adapter
        }

        @Suppress("UNCHECKED_CAST")
        fun <R : Any> forType(type: KType): ArgumentAdapter<R> =
            adapters[type]?.let { it as ArgumentAdapter<R> }
                ?: throw NoSuchElementException("No adapter configured for $type")
    }
}

private class StringAdapter : ArgumentAdapter<String> {
    override fun get(src: String): String = src

    override fun target(): KType = String::class.createType()
}

private class BooleanAdapter : ArgumentAdapter<Boolean> {
    override fun get(src: String): Boolean = src.toBoolean()

    override fun target(): KType = Boolean::class.createType()
}

