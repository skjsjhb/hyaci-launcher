package skjsjhb.mc.hyaci.net

/**
 * Represents downloadable resources.
 */
interface Artifact {
    /**
     * Remote path.
     */
    fun url(): String

    /**
     * Local path.
     */
    fun path(): String

    /**
     * Expected file size.
     *
     * This value will be used for validation, any inequality will cause the process to fail.
     * If the size cannot be determined before downloading, then 0 should be used.
     */
    fun size(): ULong

    /**
     * Checksum of the file. In the pattern of `algorithm=value`.
     *
     * An empty string indicates no checksum.
     */
    fun checksum(): String

    companion object {
        /**
         * Create an artifact with plain values.
         */
        fun of(url: String, path: String, size: ULong = 0UL, checksum: String = ""): Artifact = object : Artifact {
            override fun url(): String = url
            override fun path(): String = path
            override fun size(): ULong = size
            override fun checksum(): String = checksum
        }
    }
}