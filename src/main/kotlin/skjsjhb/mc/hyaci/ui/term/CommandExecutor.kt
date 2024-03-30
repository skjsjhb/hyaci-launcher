package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters
import skjsjhb.mc.hyaci.util.err
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMembers

/**
 * This class is responsible for recording command listeners and dispatch commands.
 */
class CommandExecutor {
    private val handlers: MutableMap<String, KCallable<Any?>> = HashMap()

    private val processors: MutableMap<String, CommandProcessor> = HashMap()

    private val usages: MutableMap<String, String> = HashMap()

    private var lastFailedCommand: Command? = null

    init {
        ServiceLoader.load(CommandProcessor::class.java).forEach { addProcessor(it) }
    }

    /**
     * Finds the handlers in the specified [CommandProcessor] and register each as a handler.
     */
    private fun addProcessor(what: CommandProcessor) {
        what::class.declaredMembers
            .filter { !it.name.startsWith("<") } // Properties
            .filter { !it.isSuspend }
            .filter { it.visibility == KVisibility.PUBLIC }
            .forEach {
                it.annotations.filterIsInstance<WithAdapters>().map { it.adapters.asIterable() }.flatten().forEach {
                    ArgumentAdapter.addAdapterClass(it)
                }

                val usage = it.annotations.filterIsInstance<Usage>().joinToString("\n") { it.value.trimIndent() }

                it.annotations
                    .filterIsInstance<CommandName>()
                    .map { it.names.asIterable() }
                    .flatten()
                    .union(setOf(it.name)) // Add method name
                    .forEach { name ->
                        handlers[name] = it
                        processors[name] = what
                        usages[name] = usage
                    }
            }
    }

    /**
     * Dispatches the given [Command] and returns its result.
     */
    fun dispatch(command: Command): Boolean {
        if (command.subject() == "retry") {
            return if (lastFailedCommand == null) {
                InteractionContext.error("There is no command to retry.")
                false
            } else {
                dispatch(lastFailedCommand!!)
            }
        } else if (command.subject() == "help") {
            val target = command.unnamed(0)
            if (target != null) {
                InteractionContext.info((usages[target]!!).trimIndent())
            } else {
                InteractionContext.info("All commands:")
                handlers.keys.forEach {
                    InteractionContext.info("- $it")
                }
                InteractionContext.info("Type 'help <command>' for command-specific usage.")
            }
            return true
        }

        val candidate = handlers[command.subject()]
        val result = if (candidate != null) {
            runCatching {
                castAndCall(command, candidate)
            }.onFailure {
                val message = it.findMostRecentMessage()
                err("Exception in command handler", it)
                InteractionContext.error("Canceled due to previous error. ($message)")
            }.getOrDefault(false)
        } else {
            InteractionContext.error("No command named ${command.subject()}")
            false
        }

        if (!result) {
            lastFailedCommand = command
        }

        return result
    }

    private fun Throwable.findMostRecentMessage(): String {
        if (localizedMessage?.isNotBlank() == true) return localizedMessage
        return cause?.findMostRecentMessage() ?: "Unknown"
    }

    private fun castAndCall(command: Command, candidate: KCallable<Any?>): Boolean =
        candidate.callBy(castArgs(command, candidate)).let { it == Unit || it == true }

    private fun castArgs(command: Command, candidate: KCallable<Any?>): Map<KParameter, Any> =
        mutableMapOf<KParameter, Any>().apply {
            candidate.parameters.stream().skip(1).forEach {
                val adapter = ArgumentAdapter.forType<Any>(it.type)
                var src = it.name?.let { command.named(it) } ?: command.unnamed(it.index - 1)
                if (src == null && !it.isOptional && it.name != null) {
                    src = InteractionContext.requestInput(it.name!!)
                }
                src?.let { s -> put(it, adapter.get(s)) }
            }

            put(candidate.parameters.first(), processors[command.subject()]!!)
        }
}