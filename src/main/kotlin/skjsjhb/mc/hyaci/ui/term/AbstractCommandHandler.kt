package skjsjhb.mc.hyaci.ui.term

abstract class AbstractCommandHandler(
    private val subject: String,
    private vararg val aliases: String
) : CommandHandler {
    constructor(subject: String) : this(subject, aliases = emptyArray())

    override fun subject(): String = subject
    override fun aliases(): Set<String> = aliases.toSet()
}