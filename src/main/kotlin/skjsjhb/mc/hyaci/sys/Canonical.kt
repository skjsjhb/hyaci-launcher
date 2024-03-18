package skjsjhb.mc.hyaci.sys

/**
 * Retrieves canonical values of the environment.
 */
object Canonical {
    /**
     * Gets the name of the OS. The value is one of `windows`, `osx` and `linux`.
     */
    fun osName(): String = System.getProperty("os.name")?.lowercase().let {
        when {
            it == null -> "linux"
            "linux" in it || "nix" in it || "sunos" in it || "bsd" in it || "unit" in it -> "linux"
            "darwin" in it || "mac" in it -> "osx"
            "windows" in it -> "windows"
            else -> "linux"
        }
    }

    /**
     * Gets the architecture.
     */
    fun osArch(): String = System.getProperty("os.arch") ?: "unknown"

    /**
     * Gets the version.
     */
    fun osVersion(): String = System.getProperty("os.version") ?: "unknown"

    /**
     * Checks if the architecture is likely to be an ARM variant.
     */
    fun isArm(): Boolean = osArch().let { it.contains("arm") || it.contains("aarch") }
}