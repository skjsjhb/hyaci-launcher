package skjsjhb.mc.hyaci.install

enum class Sources(private val value: String) {
    VANILLA_VERSION_MANIFEST("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"),
    VANILLA_RESOURCES("https://resources.download.minecraft.net");

    fun asString(): String = value
}