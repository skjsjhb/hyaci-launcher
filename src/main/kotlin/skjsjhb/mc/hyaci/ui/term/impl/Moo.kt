package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.InteractionContext
import skjsjhb.mc.hyaci.ui.term.compose.Usage

@Suppress("unused")
class Moo : CommandProcessor {
    @Usage("moo - Have you mooed today?")
    fun moo() {
        InteractionContext.run {
            info(
                """
                         (__) 
                         (oo) 
                   /------\/ 
                  / |    ||   
                 *  /\---/\ 
                    ~~   ~~   
                ...."Have you mooed today?"...
                """.trimIndent()
            )
        }
    }
}