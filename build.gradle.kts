import org.gradle.internal.os.OperatingSystem
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.2.0"

    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
}

// Hytale home path detection (OS-specific)
val hytaleHome: String = findProperty("hytale_home") as String? ?: run {
    val os = OperatingSystem.current()
    when {
        os.isWindows -> "${System.getProperty("user.home")}/AppData/Roaming/Hytale"
        os.isMacOsX -> "${System.getProperty("user.home")}/Library/Application Support/Hytale"
        os.isLinux -> listOf(
            "${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale",
            "${System.getProperty("user.home")}/.local/share/Hytale"
        ).firstOrNull { file(it).exists() }
            ?: throw GradleException("Hytale not found. Set hytale_home property.")
        else -> throw GradleException("Unsupported OS. Set hytale_home property.")
    }
}

val patchline = findProperty("patchline") as String
val includesPack = (findProperty("includes_pack") as String).toBoolean()
val loadUserMods = (findProperty("load_user_mods") as String).toBoolean()

group = property("pluginGroup") as String
version = property("pluginVersion") as String

repositories {
    mavenCentral()
    maven("https://maven.pokeskies.com/releases")
    maven("https://maven.hytale-modding.info/releases")
}

// Configuration for runtime mod dependencies (copied to build/libs for dev server)
val runtimeMods by configurations.creating

dependencies {
    // Hytale Server API - provided at runtime
    compileOnly(files("$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar"))

    // Kytale framework
    compileOnly("aster.amo:kytale:1.+")
    runtimeMods("aster.amo:kytale:1.+")
}

// Copy runtime mod dependencies to build/libs for dev server
tasks.register<Copy>("copyRuntimeMods") {
    from(runtimeMods)
    into(layout.buildDirectory.dir("libs"))
}

kotlin {
    jvmToolchain(25)
}

// Server run directory
val serverRunDir = file("$projectDir/run").apply { mkdirs() }

// Template expansion for manifest.json
tasks.processResources {
    val props = mapOf(
        "group" to project.group,
        "version" to project.version,
        "description" to (findProperty("pluginDescription") ?: ""),
        "main" to (findProperty("pluginMain") ?: "")
    )
    inputs.properties(props)
    filesMatching("manifest.json") { expand(props) }
}

// Server arguments builder
fun serverArgs(modsDir: String): List<String> {
    val args = mutableListOf(
        "--allow-op",
        "--disable-sentry",
        "--assets=$hytaleHome/install/$patchline/package/game/latest/Assets.zip"
    )
    val mods = mutableListOf(modsDir)
    if (loadUserMods) mods += "$hytaleHome/UserData/Mods"
    args += "--mods=${mods.joinToString(",")}"
    return args
}

val serverJar = "$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar"
val modsDir = layout.buildDirectory.dir("libs").get().asFile.absolutePath

// Run task that executes HytaleServer.jar
tasks.register<JavaExec>("run") {
    dependsOn(tasks.jar, "copyRuntimeMods")
    group = "application"
    description = "Runs the Hytale server with this plugin"

    mainClass.set("com.hypixel.hytale.Main")
    classpath = files(serverJar)
    args = serverArgs(modsDir)
    workingDir = serverRunDir
    standardInput = System.`in`
}

// IDEA run configuration using Gradle task
idea {
    project {
        settings {
            runConfigurations {
                create<Gradle>("HytaleServer") {
                    taskNames = listOf("run")
                }
            }
        }
    }
}
