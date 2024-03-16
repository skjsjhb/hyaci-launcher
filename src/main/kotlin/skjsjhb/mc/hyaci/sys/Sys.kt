package skjsjhb.mc.hyaci.sys

/**
 * Gets the canonical name of the current OS.
 * The value is one of `windows`, `osx` and `linux`.
 */
fun canonicalOSName(): String =
    System.getProperty("os.name")?.lowercase().let {
        when {
            it == null -> "linux"
            "linux" in it || "nix" in it || "sunos" in it || "bsd" in it || "unit" in it -> "linux"
            "darwin" in it || "mac" in it -> "osx"
            "windows" in it -> "windows"
            else -> "linux"
        }
    }

/**
 * Gets the canonical architecture of the current OS.
 */
fun canonicalOSArch(): String = System.getProperty("os.arch") ?: "unknown"

/**
 * Gets the canonical version of the current OS.
 */
fun canonicalOSVersion(): String = System.getProperty("os.version") ?: "unknown"