# Phase 0 — Project Setup & Hello World

## Goal
A compiling Kotlin plugin that loads on a local Hytale server and responds to a `/skills` chat command. This phase eliminates all toolchain friction so every future session is pure feature work.

## Prerequisites
- Java 25 JDK installed
- IntelliJ IDEA (Community Edition is sufficient)
- Hytale game installed (need `HytaleServer.jar` from the installation)
- Git

## Done Criteria
- [ ] `./gradlew build` produces a `.jar` in `build/libs/`
- [ ] Plugin loads on a local Hytale server without errors
- [ ] Typing `/skills` in-game prints a version message to chat
- [ ] Project pushed to GitHub with `.gitignore` excluding build artifacts

---

## Tasks

### Task 0.1 — Initialize Gradle project

Create the following directory structure:

```
skillsplugin/
├── src/main/
│   ├── kotlin/org/zunkree/hytale/plugins/skillsplugin/
│   │   └── HytaleSkillsPlugin.kt
│   └── resources/
│       └── manifest.json
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── .gitignore
```

### Task 0.2 — Create `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.pokeskies.com/releases")
        maven("https://maven.hytale-modding.info/releases")
    }
}

rootProject.name = "skillsplugin"
```

### Task 0.3 — Create `gradle.properties`

```properties
kotlin.code.style=official

pluginGroup=org.zunkree.hytale.plugins
pluginVersion=0.1.0
pluginDescription=Valheim-inspired skill progression system for Hytale
pluginMain=org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin

java_version=25
includes_pack=false
patchline=release
load_user_mods=false
```

### Task 0.4 — Create `build.gradle.kts`

Use the same build configuration as the storage plugin, with adjusted names:
- Project name: `skillsplugin`
- Group: `org.zunkree.hytale.plugins`
- Main class: `org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin`

Key dependencies:
- `compileOnly` for HytaleServer.jar (from local installation)
- `compileOnly` for Kytale (from Maven or local)
- `runtimeMods` configuration to copy Kytale for dev server

### Task 0.5 — Create `src/main/resources/manifest.json`

```json
{
    "Group": "${group}",
    "Name": "skillsplugin",
    "Version": "${version}",
    "Main": "${main}",
    "Description": "${description}",
    "Authors": [{"Name": "zunkree"}],
    "Dependencies": {
        "AmoAster:Kytale": "*"
    },
    "OptionalDependencies": {},
    "DisabledByDefault": false,
    "IncludesAssetPack": false
}
```

### Task 0.6 — Create `HytaleSkillsPlugin.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.dsl.command
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.plugin.JavaPluginInit

class HytaleSkillsPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    companion object {
        lateinit var instance: HytaleSkillsPlugin
            private set
    }

    override fun setup() {
        super.setup()
        instance = this

        val version = pluginVersion()
        logger.info { "HytaleSkills v$version loading..." }

        // Register /skills command
        command("skills", "View your skill levels") {
            executes { ctx ->
                ctx.sendMessage(Message.raw("HytaleSkills v$version - Valheim-inspired skill progression"))
                ctx.sendMessage(Message.raw("Skills: Swords, Axes, Bows, Spears, Clubs, Unarmed"))
                ctx.sendMessage(Message.raw("        Blocking, Mining, Woodcutting"))
                ctx.sendMessage(Message.raw("        Running, Swimming, Sneaking, Jumping"))
            }
        }

        logger.info { "HytaleSkills v$version loaded." }
    }

    override fun start() {
        super.start()
    }

    override fun shutdown() {
        logger.info { "HytaleSkills shutting down." }
        super.shutdown()
    }

    fun pluginVersion(): String = manifest.version.toString()
}
```

### Task 0.7 — Create `.gitignore`

```gitignore
# Gradle
.gradle/
build/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Runtime
run/
logs/
```

### Task 0.8 — Build and verify

```bash
# Clean stale artifacts from previous builds before verifying
./gradlew clean build
ls -la build/libs/skillsplugin-0.1.0.jar
```

> **Note:** Run `./gradlew clean` periodically (or before release builds) to remove stale JARs from `build/libs/`. Leftover artifacts like `hytale-skills-0.1.0.jar` (from an earlier project name) can cause confusion.

### Task 0.9 — Test with local server

```bash
./gradlew run
# In game, type /skills
```

---

## Troubleshooting

| Problem                     | Solution                                               |
|-----------------------------|--------------------------------------------------------|
| Kytale dependency not found | Check Maven repos, or copy from Gradle cache           |
| Plugin doesn't load         | Check manifest.json Main class path                    |
| `/skills` command not found | Verify Kytale loaded first, check command registration |
| Permission denied           | Run `op <username>` in server console                  |
