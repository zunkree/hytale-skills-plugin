import org.gradle.internal.os.OperatingSystem
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"

    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jetbrains.kotlinx.kover") version "0.9.7"
}

// Hytale home path detection (OS-specific, nullable for CI environments)
val hytaleHome: String? =
    findProperty("hytale_home") as String? ?: run {
        val os = OperatingSystem.current()
        when {
            os.isWindows -> "${System.getProperty("user.home")}/AppData/Roaming/Hytale"
            os.isMacOsX -> "${System.getProperty("user.home")}/Library/Application Support/Hytale"
            os.isLinux ->
                listOf(
                    "${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale",
                    "${System.getProperty("user.home")}/.local/share/Hytale",
                ).firstOrNull { file(it).exists() }
            else -> null
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
    // Hytale Server API - provided at runtime (unavailable in CI)
    if (hytaleHome != null) {
        compileOnly(files("$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar"))
    }

    // Kytale framework
    compileOnly("aster.amo:kytale:1.+")
    runtimeMods("aster.amo:kytale:1.+")

    // Serialization runtime — must match the version bundled in Kytale's fat JAR
    // to avoid AbstractMethodError on GeneratedSerializer.typeParametersSerializers()
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.14.5")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom("detekt.yml")
    buildUponDefaultConfig = true
}

kover {
    reports {
        filters {
            excludes {
                // Classes that depend on Hytale server types (compileOnly) and
                // cannot be unit tested without a running server
                classes(
                    "org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin",
                    "org.zunkree.hytale.plugins.skillsplugin.HexweaveRegistrationKt",
                    "org.zunkree.hytale.plugins.skillsplugin.system.*",
                    "org.zunkree.hytale.plugins.skillsplugin.persistence.*",
                    "org.zunkree.hytale.plugins.skillsplugin.listener.*",
                    "org.zunkree.hytale.plugins.skillsplugin.command.*",
                    "org.zunkree.hytale.plugins.skillsplugin.effect.CombatEffectApplier",
                    "org.zunkree.hytale.plugins.skillsplugin.effect.MovementEffectApplier",
                    "org.zunkree.hytale.plugins.skillsplugin.effect.StatEffectApplier",
                    "org.zunkree.hytale.plugins.skillsplugin.effect.GatheringEffectApplier",
                    "org.zunkree.hytale.plugins.skillsplugin.skill.PlayerSkillsComponent*",
                    "org.zunkree.hytale.plugins.skillsplugin.skill.SkillData*",
                )
            }
        }
        verify {
            rule {
                minBound(50)
            }
        }
    }
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
    val props =
        mapOf(
            "group" to project.group,
            "version" to project.version,
            "description" to (findProperty("pluginDescription") ?: ""),
            "main" to (findProperty("pluginMain") ?: ""),
        )
    inputs.properties(props)
    filesMatching("manifest.json") { expand(props) }
}

// Server-related configuration (requires Hytale installation)
if (hytaleHome != null) {
    // Server arguments builder
    fun serverArgs(modsDir: String): List<String> {
        val args =
            mutableListOf(
                "--allow-op",
                "--disable-sentry",
                "--assets=$hytaleHome/install/$patchline/package/game/latest/Assets.zip",
            )
        val mods = mutableListOf(modsDir)
        if (loadUserMods) mods += "$hytaleHome/UserData/Mods"
        args += "--mods=${mods.joinToString(",")}"
        return args
    }

    val serverJar = "$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar"
    val modsDir =
        layout.buildDirectory
            .dir("libs")
            .get()
            .asFile.absolutePath

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
}
