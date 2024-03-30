package skjsjhb.mc.hyaci.profile

import skjsjhb.mc.hyaci.container.Container
import skjsjhb.mc.hyaci.net.Artifact

/**
 * A concrete profile is a [Profile] whose paths are resolved against given [Container].
 */
class ConcreteProfile(private val base: Profile, private val container: Container) : Profile by base {
    override fun libraries(): List<Library> = base.libraries().map {
        object : Library by it {
            override fun artifact(): Artifact? = it.artifact()?.let {
                object : Artifact by it {
                    override fun path(): String = container.library(it.path()).toString()
                }
            }

            override fun nativeArtifact(): Artifact? = it.nativeArtifact()?.let {
                object : Artifact by it {
                    override fun path(): String = container.library(it.path()).toString()
                }
            }
        }
    }

    override fun assetIndexArtifact(): Artifact? = base.assetIndexArtifact()?.let {
        object : Artifact by it {
            override fun path(): String = container.assetIndex(it.path()).toString()
        }
    }

    override fun loggingArtifact(): Artifact? = base.loggingArtifact()?.let {
        object : Artifact by it {
            override fun path(): String = container.logConfig(it.path()).toString()
        }
    }

    override fun clientArtifact(): Artifact? = base.clientArtifact()?.let {
        object : Artifact by it {
            override fun path(): String = container.client(base.version()).toString()
        }
    }

    // TODO implement for mappings
    // override fun clientMappingsArtifact(): Artifact? = null
}