# Hytale Skills Plugin — Project Overview

## Project Name
`skillsplugin`

## Vision
A Valheim-inspired skill progression system for Hytale. Players improve at activities by performing them — swinging a sword levels Swords skill, chopping trees levels Woodcutting, running levels Running, etc. Higher skill levels grant bonuses like increased damage, reduced stamina usage, and improved efficiency. Death incurs a configurable skill penalty.

## Target Audience
Hytale survival players who want meaningful progression tied to their playstyle. Players who enjoy "learning by doing" mechanics rather than traditional XP-based leveling.

## Technical Stack
- **Language**: Kotlin (via Kytale framework)
- **Build System**: Gradle with Kotlin DSL
- **Server API**: Hytale Server API
- **Dependencies**: Kytale (Kotlin runtime + DSL)
- **Target Java Version**: 25
- **Distribution**: CurseForge

## Reference: Valheim Skill System

Based on research from [Game8 Valheim Guide](https://game8.co/games/Valheim/archives/321558) and [Valheim Wiki](https://valheim.fandom.com/wiki/Skills):

### Core Mechanics
- **15 skills** divided into weapon skills and utility skills
- **Level range**: 0-100 per skill
- **XP gain**: Perform the action to gain XP (must deal damage for combat skills, must have stamina)
- **Death penalty**: Lose 5% of total skill levels on death (configurable)
- **No skill drain immunity**: 10-minute protection after death prevents repeated losses

### Skill Effects at Max Level (100)
| Skill Type | Bonus |
|------------|-------|
| Weapon skills (Swords, Axes, etc.) | 2x damage multiplier |
| Blocking | 1.5x block power |
| Bows | 2x damage + instant draw |
| Run | 25% speed increase + 33% stamina reduction |
| Swim | 50% stamina reduction |
| Sneak | 75% stamina reduction + near-invisibility |
| Jump | 1.5x height increase |

### Valheim Skills List
**Weapon Skills**: Axes, Bows, Clubs, Knives, Pickaxes, Polearms, Spears, Swords, Unarmed
**Utility Skills**: Blocking, Jumping, Run, Sneak, Swim, Wood Cutting

## Hytale Adaptation

### Proposed Skills for Hytale
| Skill | Trigger | Effect |
|-------|---------|--------|
| **Swords** | Deal damage with sword | +damage (up to 2x) |
| **Axes** | Deal damage with axe | +damage (up to 2x) |
| **Bows** | Deal damage with bow | +damage, -draw time |
| **Spears** | Deal damage with spear | +damage (up to 2x) |
| **Clubs** | Deal damage with blunt weapon | +damage (up to 2x) |
| **Unarmed** | Deal damage without weapon | +damage (up to 2x) |
| **Blocking** | Block incoming damage | +block power (up to 1.5x) |
| **Mining** | Mine ore/stone | +mining speed, -stamina |
| **Woodcutting** | Chop trees | +chopping speed, -stamina |
| **Running** | Sprint | +speed, -stamina drain |
| **Swimming** | Swim | -stamina drain |
| **Sneaking** | Sneak near enemies | -detection, -stamina |
| **Jumping** | Jump | +height |

### Key Differences from Valheim
- Hytale uses an ECS architecture — skills stored as persistent components on players
- UI is server-authoritative — skill display UI must be efficient
- May need to hook into Hytale's combat/damage systems differently

## Architecture Principles

### Player Skill Data Storage
```kotlin
// Persistent ECS component on player entity
data class PlayerSkillsComponent(
    val skills: MutableMap<SkillType, SkillData> = mutableMapOf()
)

data class SkillData(
    var level: Int = 0,           // 0-100
    var xp: Float = 0f,           // Progress to next level
    var totalXp: Float = 0f       // Lifetime XP (for death penalty calculation)
)
```

### XP Formula
```
XP required for level N = baseXP * (1 + (N * scaleFactor))

Example with baseXP=100, scaleFactor=0.1:
- Level 1 requires 100 XP
- Level 50 requires 600 XP
- Level 100 requires 1100 XP
```

### Damage/Effect Formula
```
Bonus at level L = minBonus + (maxBonus - minBonus) * (L / 100)

Example for weapon damage (min=1.0, max=2.0):
- Level 0: 1.0x damage
- Level 50: 1.5x damage
- Level 100: 2.0x damage
```

### Death Penalty
```
New level = floor(currentLevel * (1 - penaltyPercent))

With 5% penalty:
- Level 100 → 95
- Level 50 → 47
- Level 10 → 9
```

## Module Structure
```
skillsplugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/main/
│   ├── kotlin/org/zunkree/hytale/plugins/skillsplugin/
│   │   ├── HytaleSkillsPlugin.kt         # Main plugin entry point
│   │   ├── skill/
│   │   │   ├── SkillType.kt              # Enum of all skill types
│   │   │   ├── SkillData.kt              # Individual skill data
│   │   │   ├── PlayerSkillsComponent.kt  # ECS component for player skills
│   │   │   └── SkillManager.kt           # XP gain, level up logic
│   │   ├── effect/
│   │   │   ├── SkillEffectApplier.kt     # Apply bonuses based on skill level
│   │   │   ├── DamageModifier.kt         # Modify damage output
│   │   │   └── StaminaModifier.kt        # Modify stamina usage
│   │   ├── listener/
│   │   │   ├── CombatListener.kt         # Track combat for weapon skills
│   │   │   ├── MovementListener.kt       # Track running, jumping, swimming
│   │   │   ├── HarvestListener.kt        # Track mining, woodcutting
│   │   │   └── DeathListener.kt          # Apply death penalty
│   │   ├── ui/
│   │   │   └── SkillsUI.kt               # Skills display menu
│   │   ├── command/
│   │   │   └── SkillsCommand.kt          # /skills command
│   │   └── config/
│   │       └── SkillsConfig.kt           # Plugin configuration (Phase 1.5)
│   └── resources/
│       ├── manifest.json
│       ├── config.json
│       └── Server/
│           └── UI/
│               └── skills_menu.ui
└── docs/
    └── phases/                           # This project plan
```

## Phase Summary
| Phase | Goal | Depends On | Deliverable |
|-------|------|------------|-------------|
| 0 | Project setup + hello world | Nothing | Compiling plugin, `/skills` command works |
| 1 | Skill data model + persistence | Phase 0 | SkillType enum, PlayerSkillsComponent, save/load |
| 1.5 | Configuration system | Phase 1 | SkillsConfig, config.json, tunable values available |
| 2 | XP gain + leveling | Phase 1.5 | Gain XP from actions, level up with notifications |
| 3 | Skill effects | Phase 2 | Damage/stamina bonuses applied based on skill level |
| 4 | Death penalty | Phase 3 | Skill loss on death, immunity period |
| 5 | Skills UI | Phase 4 | `/skills` opens menu showing all skills + levels |
| 6 | Polish + admin + release | Phase 5 | Admin commands, performance optimization, CurseForge release |

## Key Constraints
- **Event hooking**: Must find correct Hytale events for damage dealt, blocks broken, movement, death
- **Performance**: Skill checks happen frequently (every attack, every movement tick) — must be efficient
- **UI latency**: Skills UI is server round-trip; cache skill data client-side if possible via HUD elements
- **Balance**: XP rates and bonuses need tuning; expose all values in config
- **API Research**: Code examples throughout these docs represent *design intent*, not confirmed APIs. Hytale and Kytale are evolving — event types, ECS methods, UI systems, and command argument types all need discovery and validation against actual SDK documentation before implementation. Each phase includes a "Research Required" section listing the APIs to confirm.

## Configuration (config.json)
```json
{
    "maxSkillLevel": 100,
    "baseXpPerAction": 1.0,
    "xpScaleFactor": 0.1,
    "deathPenaltyPercent": 0.05,
    "deathImmunitySeconds": 600,
    "restedXpBonus": 0.5,
    "skillEffects": {
        "SWORDS": { "minDamageMultiplier": 1.0, "maxDamageMultiplier": 2.0 },
        "RUNNING": { "minSpeedBonus": 0.0, "maxSpeedBonus": 0.25, "minStaminaReduction": 0.0, "maxStaminaReduction": 0.33 }
    }
}
```

## Community Resources
- HytaleModding.dev — guides on plugins, ECS, events
- Kytale GitHub — Kotlin DSL patterns
- HytaleModding Discord — active modding community
- Valheim Wiki — reference implementation details
