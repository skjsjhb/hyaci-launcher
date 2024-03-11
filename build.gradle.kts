plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("idea")
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
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.22.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.h2database:h2:2.2.224")

    runtimeOnly("org.fusesource.jansi:jansi:2.4.1")

    testImplementation("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dlog4j.skipJansi=false")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "skjsjhb.mc.hyaci.Main"
    applicationDefaultJvmArgs = listOf("-Dlog4j.skipJansi=false")
}
