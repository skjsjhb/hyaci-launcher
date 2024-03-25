package skjsjhb.mc.hyaci.util

enum class Sources(val value: String) {
    VANILLA_VERSION_MANIFEST("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"),
    VANILLA_RESOURCES("https://resources.download.minecraft.net"),
    XBL_API("https://user.auth.xboxlive.com/user/authenticate"),
    XSTS_API("https://xsts.auth.xboxlive.com/xsts/authorize"),
    OAUTH_API("https://login.live.com/oauth20_token.srf"),
    MOJANG_LOGIN_API("https://api.minecraftservices.com/authentication/login_with_xbox"),
    MOJANG_PROFILE_API("https://api.minecraftservices.com/minecraft/profile"),
    MOJANG_JRE_MANIFEST("https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json")
}