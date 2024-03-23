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
fun forkClass(className: String, args: List<String> = emptyList(), vmArgs: List<String> = emptyList()): Process =
    ProcessBuilder().apply {
        Path.of(System.getProperty("java.home"), "bin", "java").normalize().toAbsolutePath().toString().let {
            debug("Forking class $className using $it")
            command(listOf(it, "-cp", System.getProperty("java.class.path")) + vmArgs + listOf(className) + args)
        }
    }.start()
