package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.util.err
import java.util.*

object CommandExecutor {

    private val handlers: MutableMap<String, CommandHandler> = mutableMapOf()

    private var lastFailedCommand: Command? = null

    init {
        ServiceLoader.load(CommandHandler::class.java).forEach {
            handle(it)
            it.aliases().forEach { a -> alias(a, it.subject()) }
        }
        handle(HelpCommandHandler)
    }

    /**
     * Registers a method as a command handler.
     */
    fun handle(what: CommandHandler) {
        handlers[what.subject()] = what
    }

    fun alias(name: String, target: String) {
        handlers[target]?.let { handlers[name] = it }
    }

    fun commands(): Set<String> = handlers.keys

    fun dispatch(command: Command): Boolean {
        if (command.subject() == "retry") {
            if (lastFailedCommand == null) {
                terror("There is no command to retry.")
                return false
            }
            return dispatch(lastFailedCommand!!)
        }

        return (handlers[command.subject()] ?: FallbackCommandHandler).run {
            runCatching { handle(command) }
                .onFailure {
                    err("Exception in command handler", it)
                    terror("Canceled due to previous error. (${it.localizedMessage})")
                }
                .getOrDefault(false)
                .also { if (!it) lastFailedCommand = command } // Mark the command as failed
        }
    }
}

private object FallbackCommandHandler : CommandHandler {
    override fun handle(command: Command): Boolean {
        terror("No command named ${command.subject()}")
        return false
    }

    override fun subject(): String = ""
}

private object HelpCommandHandler : CommandHandler {
    override fun handle(command: Command): Boolean {
        tinfo("All available commands:")
        CommandExecutor.commands().forEach { tinfo(it) }
        return true
    }

    override fun subject(): String = "help"
}