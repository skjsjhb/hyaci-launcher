package skjsjhb.mc.hyaci.launch

import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.sys.dataPathOf
import java.nio.file.Files
import java.nio.file.Path

object JreManager {
    /**
     * Resolves the `JAVA_HOME` path of the given JRE component.
     */
    fun getJavaHome(componentName: String): Path = dataPathOf("runtimes/$componentName")

    /**
     * Resolves the path to JRE executable file.
     */
    fun getJavaExecutable(componentName: String): Path =
        getJavaHome(componentName).resolve(
            when (Canonical.osName()) {
                "osx" -> "jre.bundle/Contents/Home/bin/java"
                "windows" -> "bin/java.exe"
                else -> "bin/java"
            }
        )

    /**
     * Checks whether a JRE component has been installed.
     *
     * This method performs a brief test by checking the existence of the executable file. Assuming that
     * the previous installation was successful.
     */
    fun hasComponent(componentName: String): Boolean = Files.exists(getJavaExecutable(componentName))
}