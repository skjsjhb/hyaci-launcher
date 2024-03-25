plugins {
    kotlin("jvm") version "1.9.23"
    id("idea")
    id("org.jetbrains.dokka") version "1.9.20"
    id("application")
}

group = "skjsjhb.mc.hyaci"
version = "1.0"

idea {
    module {
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("me.friwi:jcefmaven:122.1.10")
}

application {
    mainClass = "skjsjhb.mc.hyaci.auth.OAuthHelper"
    applicationDefaultJvmArgs = listOf(
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
    )
}