import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jetbrains.dokka") version "1.9.20"
    id("idea")
    id("application")
    id("org.panteleyev.jpackageplugin") version "1.6.0"
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
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.h2database:h2:2.2.224")
    implementation("com.github.jponge:lzma-java:1.3")
    implementation("com.microsoft:credential-secure-storage:1.0.0")
    implementation("org.fusesource.jansi:jansi:1.18")
    implementation(kotlin("reflect"))

    runtimeOnly(project(":oauth-helper"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}



tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dlog4j.skipJansi=false")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "skjsjhb.mc.hyaci.Main"
    applicationDefaultJvmArgs = listOf("-Dlog4j.skipJansi=false")
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}


task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into(layout.buildDirectory.dir("jars"))
}

task("copyJar", Copy::class) {
    from(tasks.jar).into(layout.buildDirectory.dir("jars"))
}

tasks.register<JPackageTask>("installer") {
    description = "Create installer using JPackage."

    windows {
        type = ImageType.MSI
        winPerUserInstall = true
        winMenu = true
    }
}

tasks.register<JPackageTask>("portable") {
    description = "Create portable executable file using JPackage."
    type = ImageType.APP_IMAGE
}

tasks.withType<JPackageTask> {
    group = "distribution"

    dependsOn("jar", "copyDependencies", "copyJar")

    appName = "Hyaci Launcher"
    vendor = "The Hyaci Launcher Project"
    appVersion = project.version.toString()
    copyright = "Copyright (C) 2024 Ted \"skjsjhb\" Gao"

    runtimeImage = System.getenv("HYACI_BUILD_JAVA_IMAGE") ?: System.getProperty("java.home")

    mainClass = "skjsjhb.mc.hyaci.Main"
    mainJar = tasks.jar.get().archiveFileName.get()

    input = layout.buildDirectory.dir("jars").get().asFile.path

    destination = layout.buildDirectory.dir("dist").get().asFile.path

    javaOptions = listOf("-Dfile.encoding=UTF-8", "-Dlog4j.skipJansi=false")

    windows {
        doFirst {
            exec {
                workingDir = layout.buildDirectory.asFile.get()
                isIgnoreExitValue = true
                commandLine("cmd", "/C", "rmdir /Q /S \"dist\\Hyaci Launcher\"")
            }
        }
        winConsole = true
    }

    mac {
        doFirst {
            exec {
                workingDir = layout.buildDirectory.asFile.get()
                commandLine("rm", "-rf", "dist/Hyaci Launcher")
            }
        }
        javaOptions = listOf("-XstartOnFirstThread", "-Dfile.encoding=UTF-8", "-Dlog4j.skipJansi=false")
        macPackageIdentifier = "skjsjhb.mc.hyaci"
    }

    linux {
        doFirst {
            exec {
                workingDir = layout.buildDirectory.asFile.get()
                commandLine("rm", "-rf", "dist/Hyaci Launcher")
            }
        }
    }
}
