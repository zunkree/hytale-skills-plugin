# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Hytale server plugin implementing a Valheim-inspired skill progression system. Players improve skills by performing actions (combat, gathering, movement). Higher skill levels provide gameplay bonuses. Death incurs a configurable skill penalty.

**Current state**: Documentation complete, project scaffold ready for implementation.

## Build Commands

```bash
# Build the plugin JAR
./gradlew build

# Run the server (also available as HytaleServer run configuration in IDEA)
./gradlew run

# Build outputs to build/libs/
```

## Key Configuration Files

- `gradle.properties` - Version, Java version (25), patchline, plugin metadata
- `src/main/resources/manifest.json` - Plugin metadata (auto-updated by Gradle)
- `settings.gradle.kts` - Project name (used for JAR filename)

## Architecture Notes

### Hytale Plugin Model
- **Server-side only**: All mods are server plugins. No client modification possible.
- **Plugin lifecycle**: Constructor → `setup()` → `start()` → `shutdown()`
- **Entry point**: `manifest.json` `Main` field points to class extending `KotlinPlugin`

### ECS Persistence
- `addComponent()` - temporary, lost on restart
- `putComponent()` - **persistent**, survives restarts (use for skill data)
- Components need `BuilderCodec<T>` for serialization

### Skill System Design
- 13 skills: Swords, Axes, Bows, Spears, Clubs, Unarmed, Blocking, Mining, Woodcutting, Running, Swimming, Sneaking, Jumping
- Level range: 0-100
- XP gained by performing related actions
- Effects scale linearly from level 0 to 100
- Death penalty: 5% skill loss, 10-minute immunity

## Development Setup

1. Hytale must be installed via official launcher
2. Java 25 required
3. Kytale dependency required (copied to build/libs by Gradle)
4. First run requires server authentication

## Planned Architecture (docs/)

See `docs/00-PROJECT-OVERVIEW.md` for full roadmap. Target structure:
- `skill/` - SkillType, SkillData, PlayerSkillsComponent, SkillManager
- `effect/` - Skill effect modifiers (damage, stamina, speed)
- `listener/` - Event listeners for XP gain and death penalty
- `ui/` - Skills menu UI
- `command/` - /skills command
- `config/` - Plugin configuration

## Reference: Valheim Mechanics

Based on research from:
- [Game8 Valheim Skills Guide](https://game8.co/games/Valheim/archives/321558)
- [Valheim Wiki - Skills](https://valheim.fandom.com/wiki/Skills)

Key mechanics adapted:
- Learning by doing (XP from actions)
- 2x damage multiplier at max weapon skill
- Stamina reduction for movement skills
- 5% skill loss on death with immunity period
