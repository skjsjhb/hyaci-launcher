package skjsjhb.mc.hyaci.util

enum class Sources(val value: String) {
    VANILLA_VERSION_MANIFEST("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"),
    VANILLA_RESOURCES("https://resources.download.minecraft.net"),
    XBL_API("https://user.auth.xboxlive.com/user/authenticate"),
    XSTS_API("https://xsts.auth.xboxlive.com/xsts/authorize"),
    OAUTH_API("https://login.live.com/oauth20_token.srf"),
    OAUTH_WEB("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"),
    MOJANG_LOGIN_API("https://api.minecraftservices.com/authentication/login_with_xbox"),
    MOJANG_PROFILE_API("https://api.minecraftservices.com/minecraft/profile")
}