package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.tinfo

@Suppress("unused")
class Moo : CommandProcessor {
    @Usage("moo - Have you mooed today?")
    fun moo() {
        tinfo(
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