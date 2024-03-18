package skjsjhb.mc.hyaci.vfs

import java.nio.file.Path

/**
 * Represents a virtual filesystem managing game resources.
 *
 * There exists a number of different ways to organize the resource layout,
 * each with different path resolution strategies.
 * This interface covers the implementation details, leaving a common abstraction.
 *
 * Method names of this class are shortened for simplicity.
 * Without clarification, it can be assumed that they perform path resolution and return the JVM [Path] representation.
 */
interface Vfs {
    /**
     * Gets the path to the version profile.
     */
    fun profile(id: String): Path

    /**
     * Gets the path to the client jar file.
     */
    fun client(id: String): Path

    /**
     * Gets the path to the natives unpack directory.
     */
    fun natives(id: String): Path

    /**
     * Gets the path to a library.
     */
    fun library(path: String): Path

    /**
     * Gets the asset root.
     */
    fun assetRoot(): Path

    /**
     * Gets the legacy asset root.
     */
    fun assetRootLegacy(): Path

    /**
     * Gets the path to resource-mapped asset root.
     */
    fun assetRootMapToResources(): Path

    /**
     * Gets the path to an asset file.
     */
    fun asset(hash: String): Path

    /**
     * Gets the path to legacy asset file.
     *
     * This method exists to maintain compatibility with pre-1.6 versions.
     */
    fun assetLegacy(fileName: String): Path

    /**
     * Gets the path to resource-mapped asset file.
     *
     * This method exists to maintain compatibility with pre-1.6 versions.
     */
    fun assetMapToResources(fileName: String): Path

    /**
     * Gets the path to the given asset index.
     */
    fun assetIndex(id: String): Path

    /**
     * Gets the path to the logging configuration file.
     */
    fun logConfig(id: String): Path

    /**
     * Gets the game directory.
     */
    fun gameDir(): Path

    /**
     * Resolves arbitrary path relative to the random storage root.
     */
    fun resolve(rel: String): Path
}