package skjsjhb.mc.hyaci.install

/**
 * Represents generic install task.
 */
interface Installer {
    /**
     * Runs the installation task. Either returns silently when succeeded, or throws and exception.
     */
    fun install()
}