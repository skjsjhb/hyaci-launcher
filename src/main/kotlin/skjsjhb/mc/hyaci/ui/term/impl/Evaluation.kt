package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.ui.term.*
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import java.io.FileReader
import java.io.Reader
import java.net.URI

@Suppress("unused")
class Evaluation : CommandProcessor {
    @CommandName("ef")
    @Usage(
        """
        ef <path> - Execute launch script from given file.
            path - Path to the file that contains the script.
    """
    )
    fun execFile(path: String) {
        warnDanger()
        exec(FileReader(path))
    }

    @CommandName("eu")
    @Usage(
        """
        eu <url> - Execute launch script from given URL.
            url - URL to the script.
    """
    )
    fun execUrl(url: String) {
        warnDanger()
        exec(URI(url).toURL().openStream().reader())
    }

    private fun warnDanger() {
        twarn("Launch script can be EXTREMELY DANGEROUS if its content is unknown.")
        twarn("Including but not limited to data corruption, credentials leak and malware.")
        twarn("DO NOT execute launch scripts from untrusted source!")
        requireConfirm("I know what I'm doing!")
    }

    private fun exec(src: Reader) {
        val executor = CommandExecutor()
        while (true) {
            src.readCommand().let {
                if (it == null) return
                tinfo(">>> $it")
                executor.dispatch(it)
            }
        }
    }
}