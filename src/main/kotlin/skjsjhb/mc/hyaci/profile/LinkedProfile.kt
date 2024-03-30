package skjsjhb.mc.hyaci.profile

import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.util.debug

/**
 * A composite profile which links two profiles together.
 */
class LinkedProfile(private val base: Profile, private val head: Profile) : Profile {
    // Merge two nullable parts
    private fun <T> merge(b: T, h: T): T =
        if (h is String) { // Merge candidates must be of the same type
            h.ifBlank { b }
        } else {
            h ?: b
        }

    override fun id(): String = merge(base.id(), head.id())

    override fun version(): String = merge(head.version(), base.version()) // Version of the base profile wins

    override fun libraries(): List<Library> = head.libraries() + base.libraries()

    override fun inheritsFrom(): String = base.inheritsFrom()

    override fun jvmArgs(): List<Argument> = base.jvmArgs() + head.jvmArgs()

    override fun gameArgs(): List<Argument> = base.gameArgs() + head.gameArgs()

    override fun mainClass(): String = merge(base.mainClass(), head.mainClass())

    override fun assetId(): String = merge(base.assetId(), head.assetId())

    override fun assetIndexArtifact(): Artifact? = merge(base.assetIndexArtifact(), head.assetIndexArtifact())

    override fun loggingArtifact(): Artifact? = merge(base.loggingArtifact(), head.loggingArtifact())

    override fun jreComponent(): String = merge(base.jreComponent(), head.jreComponent())

    override fun jreVersion(): Int = head.jreVersion().takeIf { it > 0 } ?: base.jreVersion()

    override fun clientArtifact(): Artifact? = merge(base.clientArtifact(), head.clientArtifact())

    override fun clientMappingsArtifact(): Artifact? =
        merge(base.clientMappingsArtifact(), head.clientMappingsArtifact())

    override fun versionType(): String = merge(base.versionType(), head.versionType())

    companion object {
        /**
         * Tries to link the given profiles.
         *
         * If the former is null, then this method trivially returns the latter, and vice versa.
         * If both profiles are non-null, then this method creates a [LinkedProfile] with the former inherits the latter.
         */
        fun of(base: Profile?, head: Profile?): Profile =
            when {
                head == null && base == null -> throw IllegalArgumentException("Linking two null profiles")
                head == null -> base!!
                base == null -> head
                else -> {
                    debug("Patched ${head.id()} -> ${base.id()}")
                    LinkedProfile(base, head)
                }
            }
    }
}