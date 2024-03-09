package skjsjhb.mc.hyaci.sys

import skjsjhb.mc.hyaci.util.debug
import java.nio.file.Path

/**
 * Forks a new runtime and executes the given class as the main class.
 *
 * This method depends heavily on the presentation of JVM.
 * Therefore, this method cannot be invoked on a non-JVM platform (e.g., GraalVM Native, Kotlin Native, etc.)
 * Compressions and encapsulations may also break it, test before use.
 *
 * @param className The new main class. The class must contain a static `main` method to be applicable to run.
 * @param args Arguments for the new instance.
 */
fun forkClass(className: String, args: List<String> = emptyList()): Process =
    ProcessBuilder().apply {
        val bin = Path.of(System.getProperty("java.home"), "bin", "java").normalize().toAbsolutePath().toString()
        debug("Forking class $className using $bin")
        command(listOf(bin, "-cp", System.getProperty("java.class.path"), className) + args)
    }.start()
