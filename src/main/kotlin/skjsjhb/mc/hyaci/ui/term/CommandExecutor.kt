package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.util.err
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMembers

object CommandExecutor {
    private val handlers: MutableMap<String, KCallable<Any?>> = HashMap()

    private val processors: MutableMap<String, CommandProcessor> = HashMap()

    private val usage: MutableMap<String, String> = HashMap()

    private var lastFailedCommand: Command? = null

    init {
        ServiceLoader.load(CommandProcessor::class.java).forEach { addProcessor(it) }
    }

    fun addProcessor(what: CommandProcessor) {
        what::class.declaredMembers
            .filter {
                !it.name.startsWith("<") && !it.isSuspend && it.visibility == KVisibility.PUBLIC
            }.forEach {
                it.annotations.find { it is WithAdapters }
                    ?.let { it as WithAdapters }?.adapters?.forEach { ArgumentAdapter.addAdapterClass(it) }

                val usageString = it.annotations.find { it is Usage }
                    ?.let { it as Usage }?.value ?: ""

                val annotatedNames = it.annotations.find { it is CommandName }?.let { it as CommandName }?.names
                if (annotatedNames != null) {
                    annotatedNames.forEach { name ->
                        handlers[name] = it
                        processors[name] = what
                        usage[name] = usageString
                    }
                } else {
                    handlers[it.name] = it
                    processors[it.name] = what
                    usage[it.name] = usageString
                }
            }
    }

    fun dispatch(command: Command): Boolean {
        if (command.subject() == "retry") {
            return if (lastFailedCommand == null) {
                terror("There is no command to retry.")
                false
            } else {
                dispatch(lastFailedCommand!!)
            }
        } else if (command.subject() == "help") {
            val target = command.unnamed(0)
            if (target != null) {
                tinfo((usage[target]!!).trimIndent())
            } else {
                tinfo("All commands:")
                handlers.keys.forEach {
                    tinfo("- $it")
                }
                tinfo("Type 'help <command>' for command-specific usage.")
            }
            return true
        }

        val candidate = handlers[command.subject()]
        val result = if (candidate != null) {
            runCatching {
                castAndCall(command, candidate)
            }.onFailure {
                if (it.cause?.message == "Canceled") {
                    terror("Canceled.")
                } else {
                    err("Exception in command handler", it)
                    terror("Canceled due to previous error. (${it.localizedMessage})")
                }
            }.getOrDefault(false)
        } else {
            terror("No command named ${command.subject()}")
            false
        }

        if (!result) {
            lastFailedCommand = command
        }

        return result
    }

    private fun castAndCall(command: Command, candidate: KCallable<Any?>): Boolean =
        candidate.callBy(castArgs(command, candidate)).let { it == Unit || it == true }

    private fun castArgs(command: Command, candidate: KCallable<Any?>): Map<KParameter, Any> =
        mutableMapOf<KParameter, Any>().apply {
            candidate.parameters.stream().skip(1).forEach {
                val adapter = ArgumentAdapter.forType<Any>(it.type)
                var src = it.name?.let { command.named(it) } ?: command.unnamed(it.index - 1)
                if (src == null && !it.isOptional && it.name != null) {
                    src = askMore(it.name!!)
                }
                src?.let { s -> put(it, adapter.get(s)) }
            }

            put(candidate.parameters.first(), processors[command.subject()]!!)
        }
}