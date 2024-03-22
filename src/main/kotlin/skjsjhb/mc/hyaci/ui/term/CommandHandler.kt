package skjsjhb.mc.hyaci.ui.term

interface CommandHandler {
    fun handle(command: Command): Boolean

    fun subject(): String

    fun aliases(): Set<String> = emptySet()
}

