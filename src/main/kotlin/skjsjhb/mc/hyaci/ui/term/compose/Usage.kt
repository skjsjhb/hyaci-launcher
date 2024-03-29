package skjsjhb.mc.hyaci.ui.term.compose

/**
 * Describes how to use the command. The content will be printed as help message.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Usage(val value: String)
