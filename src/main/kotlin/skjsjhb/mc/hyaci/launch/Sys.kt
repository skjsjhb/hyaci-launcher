package skjsjhb.mc.hyaci.launch

/**
 * Gets the canonical name of the current OS.
 * The value is one of `windows`, `osx` and `linux`.
 */
fun canonicalOSName(): String =
    System.getProperty("os.name")?.lowercase().let {
        when {
            it == null -> "linux"
            "darwin" in it || "mac" in it -> "osx"
            "windows" in it -> "windows"
            else -> "linux"
        }
    }
