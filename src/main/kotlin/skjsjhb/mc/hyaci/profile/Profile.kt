package skjsjhb.mc.hyaci.profile

import kotlinx.serialization.json.Json
import skjsjhb.mc.hyaci.container.Container
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.info
import skjsjhb.mc.hyaci.util.warn
import java.nio.file.Files

/**
 * Holding properties of game profiles.
 *
 * Due to historical reasons, the game contains formats which are not compatible with each other.
 * This interface tries to provide a standard approach of retrieving properties without the need of investigating the
 * detailed format information.
 */
interface Profile {
    /**
     * ID of the profile.
     */
    fun id(): String

    /**
     * Game version name.
     */
    fun version(): String

    /**
     * Libraries of the profile.
     */
    fun libraries(): List<Library>

    /**
     * Gets the dependency of this profile.
     */
    fun inheritsFrom(): String

    /**
     * Gets the JVM arguments.
     *
     * Arguments for logging and other extra JVM arguments are also included.
     */
    fun jvmArgs(): List<Argument>

    /**
     * Gets the game arguments.
     */
    fun gameArgs(): List<Argument>

    /**
     * Gets the main class name.
     */
    fun mainClass(): String

    /**
     * Gets the asset ID.
     */
    fun assetId(): String

    /**
     * Gets the asset index artifact.
     */
    fun assetIndexArtifact(): Artifact?

    /**
     * Gets the logging artifact.
     */
    fun loggingArtifact(): Artifact?

    /**
     * Gets the name of JRE component.
     */
    fun jreComponent(): String

    /**
     * Gets the suggested JRE version.
     */
    fun jreVersion(): Int

    /**
     * Gets the client artifact.
     */
    fun clientArtifact(): Artifact?

    /**
     * Gets the client mappings artifact.
     */
    fun clientMappingsArtifact(): Artifact?

    /**
     * Gets the version type.
     */
    fun versionType(): String

    companion object {

        /**
         * Loads a profile from given [Container] instance and resolve it.
         *
         * Profiles are parsed and linked.
         * Dependencies are fetched from the given [Container].
         *
         * @param id The ID of the profile to be loaded.
         * @param container A [Container] filesystem containing the profile.
         */
        fun load(id: String, container: Container): ConcreteProfile =
            ConcreteProfile(load(id) { Files.readString(container.profile(it)) }, container)

        /**
         * Loads a set of [Profile]s using given getter.
         *
         * Profiles are parsed and linked.
         * Dependencies are fetched using the specified getter method.
         *
         * @param id The ID of the profile to be loaded.
         * @param getter A getter for retrieving profile content by the given ID.
         */
        fun load(id: String, getter: (id: String) -> String): Profile {
            info("Loading profile $id")

            var currentDep = id
            val visitedDep = mutableSetOf<String>()

            var head: Profile? = null

            // Repeatedly links head -> currentDep
            while (currentDep.isNotBlank()) {
                debug("Parsing profile $currentDep")

                // Detect circular dependency
                if (currentDep in visitedDep) {
                    warn("Circular dependency detected, dependency $currentDep, early returning.")
                    break
                }

                visitedDep.add(currentDep)

                // Link profile
                val base = JsonProfile(Json.parseToJsonElement(getter(currentDep)))
                head = LinkedProfile.of(base, head)
                currentDep = base.inheritsFrom()
            }
            return head!!
        }
    }
}

/**
 * Represents metadata of a library.
 */
interface Library : RuleManaged {
    /**
     * Name of the library.
     */
    fun name(): String

    /**
     * Gets the artifact of the library.
     */
    fun artifact(): Artifact?

    /**
     * Gets the native artifact of the library.
     */
    fun nativeArtifact(): Artifact?
}

/**
 * Generic argument representation.
 */
interface Argument : RuleManaged {
    /**
     * The values of the argument when it's applied.
     */
    fun values(): List<String>
}

