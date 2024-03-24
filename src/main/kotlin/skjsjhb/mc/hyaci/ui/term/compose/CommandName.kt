package skjsjhb.mc.hyaci.ui.term.compose

/**
 * States the name of the command. The name specified here will be used (instead of the function name).
 */
@Target(AnnotationTarget.FUNCTION)
annotation class CommandName(vararg val names: String)
