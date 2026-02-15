# Hytale Skills Plugin — Project Overview

## Project Name
`skillsplugin`

## Vision
A Valheim-inspired skill progression system for Hytale. Players improve at activities by performing them — swinging a sword levels Swords skill, chopping trees levels Woodcutting, running levels Running, etc. Higher skill levels grant bonuses like increased damage, reduced stamina usage, and improved efficiency. Death incurs a configurable skill penalty.

## Target Audience
Hytale survival players who want meaningful progression tied to their playstyle. Players who enjoy "learning by doing" mechanics rather than traditional XP-based leveling.

## Technical Stack
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Server API**: Hytale Server API (native Hypixel APIs)
- **Dependencies**: None (standalone plugin)
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
| Skill Type                         | Bonus                                      |
|------------------------------------|--------------------------------------------|
| Weapon skills (Swords, Axes, etc.) | 2x damage multiplier                       |
| Blocking                           | 50% stamina reduction                      |
| Bows                               | 2x damage + instant draw                   |
| Run                                | 25% speed increase + 33% stamina reduction |
| Swim                               | 50% stamina reduction                      |
| Sneak                              | 75% stamina reduction + near-invisibility  |
| Jump                               | 1.5x height increase                       |

### Valheim Skills List
**Weapon Skills**: Axes, Bows, Clubs, Knives, Pickaxes, Polearms, Spears, Swords, Unarmed
**Utility Skills**: Blocking, Jumping, Run, Sneak, Swim, Wood Cutting

## Hytale Adaptation

### Hytale Skills (15 total)
| Skill           | Category  | Trigger                            | Effect                             |
|-----------------|-----------|------------------------------------|------------------------------------|
| **Swords**      | Weapon    | Deal damage with sword/longsword   | +damage (up to 2x)                 |
| **Daggers**     | Weapon    | Deal damage with daggers           | +damage (up to 2x)                 |
| **Axes**        | Weapon    | Deal damage with axe/battleaxe     | +damage (up to 2x)                 |
| **Bows**        | Weapon    | Deal damage with shortbow/crossbow | +damage, -draw time                |
| **Spears**      | Weapon    | Deal damage with spear             | +damage (up to 2x)                 |
| **Clubs**       | Weapon    | Deal damage with mace/club         | +damage (up to 2x)                 |
| **Unarmed**     | Weapon    | Deal damage without weapon         | +damage (up to 2x)                 |
| **Blocking**    | Utility   | Block incoming damage              | -blocking stamina cost (up to 50%) |
| **Mining**      | Gathering | Damage ore/stone blocks            | +mining speed, -stamina            |
| **Woodcutting** | Gathering | Damage wood blocks                 | +chopping speed, -stamina          |
| **Running**     | Movement  | Sprint                             | +speed, -stamina drain             |
| **Swimming**    | Movement  | Swim                               | +swim speed, -stamina drain        |
| **Diving**      | Movement  | Submerge in fluid                  | +oxygen capacity                   |
| **Sneaking**    | Movement  | Crouch while moving                | -detection, -stamina               |
| **Jumping**     | Movement  | Jump                               | +height                            |

### Key Differences from Valheim
- Hytale uses an ECS architecture — skills stored as persistent components on players
- UI is server-authoritative — skill display UI must be efficient
- May need to hook into Hytale's combat/damage systems differently

## Architecture Principles

### Player Skill Data Storage
```kotlin
// Persistent ECS component on player entity (Component<EntityStore>)
// Uses putComponent() for persistence across restarts
// Serialized via BuilderCodec<T> to BSON
class PlayerSkillsComponent : Component<EntityStore> {
    val skills: MutableMap<SkillType, SkillData>  // All 15 skills initialized to level 0
    var deathImmunityUntil: Long = 0L             // Timestamp for death penalty immunity
}

data class SkillData(
    var level: Int = 0,           // 0-100
    var totalXP: Double = 0.0     // Cumulative XP from level 0
)
```

### XP Formula
```
XP required for level N = baseXpPerAction * 100 * (1 + (N-1) * xpScaleFactor)

Example with baseXpPerAction=1.0, xpScaleFactor=1.0:
- Level 1 requires 100 XP
- Level 50 requires 5000 XP
- Level 100 requires 10000 XP

Cumulative XP is tracked per skill (totalXP field).
Level-up occurs when totalXP >= cumulativeXpForLevel(currentLevel + 1).
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

Default: 10% penalty with 300s (5 min) immunity:
- Level 100 → 90
- Level 50 → 45
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
│   │   ├── SkillsPlugin.kt              # Main plugin entry (JavaPlugin)
│   │   ├── bootstrap/
│   │   │   ├── PluginApplication.kt     # Wiring/DI, system registration
│   │   │   └── RuntimeState.kt          # Shutdown state (active players)
│   │   ├── skill/
│   │   │   ├── SkillType.kt             # Enum: 15 skills with categories
│   │   │   ├── SkillCategory.kt         # Enum: COMBAT, GATHERING, MOVEMENT, WEAPON, UTILITY
│   │   │   ├── SkillData.kt             # Level + cumulative XP with BuilderCodec
│   │   │   └── PlayerSkillsComponent.kt # ECS Component<EntityStore> with codec
│   │   ├── xp/
│   │   │   ├── XpCurve.kt              # Quadratic XP formula + level progress
│   │   │   └── XpService.kt            # Grant XP, level-up check, notifications
│   │   ├── persistence/
│   │   │   └── SkillRepository.kt       # ECS component get/put via Ref<EntityStore>
│   │   ├── resolver/
│   │   │   ├── WeaponSkillResolver.kt   # Item ID prefix → SkillType
│   │   │   ├── BlockSkillResolver.kt    # Block ID prefix → Mining/Woodcutting
│   │   │   └── MovementXpPolicy.kt      # Movement state → XP grants
│   │   ├── system/
│   │   │   ├── DamageContext.kt         # Data class for damage system context
│   │   │   ├── SkillEffectDamageSystem.kt # DamageEventSystem: effects (before ApplyDamage)
│   │   │   ├── CombatXpDamageSystem.kt  # DamageEventSystem: XP grants (after ApplyDamage)
│   │   │   ├── BlockDamageXpSystem.kt   # EntityEventSystem: gathering XP + effects
│   │   │   └── MovementTickSystem.kt    # EntityTickingSystem: movement XP + effects
│   │   ├── effect/
│   │   │   ├── CombatEffectApplier.kt   # Damage/blocking effects
│   │   │   ├── MovementEffectApplier.kt # Speed/jump effects
│   │   │   ├── StatEffectApplier.kt     # Stamina/oxygen effects
│   │   │   ├── GatheringEffectApplier.kt # Mining/woodcutting speed
│   │   │   └── SkillEffectCalculator.kt # Linear interpolation from config
│   │   ├── listener/
│   │   │   ├── CombatListener.kt        # Weapon XP from damage dealt
│   │   │   ├── BlockingListener.kt      # Blocking XP from damage blocked
│   │   │   ├── HarvestListener.kt       # DamageBlockEvent for gathering XP
│   │   │   ├── MovementListener.kt      # Tick-based movement XP tracking
│   │   │   └── PlayerLifecycleListener.kt # PlayerReady/Disconnect → load/save
│   │   ├── command/
│   │   │   └── SkillsCommand.kt         # AbstractPlayerCommand for /skills
│   │   ├── config/
│   │   │   ├── SkillsConfig.kt          # Nested config data classes
│   │   │   ├── SkillsConfigCodec.kt     # BuilderCodec definitions
│   │   │   └── SkillsConfigValidator.kt # Config validation
│   │   └── extension/
│   │       └── LoggerExtensions.kt      # HytaleLogger Kotlin extensions (debug/info/error)
│   └── resources/
│       └── manifest.json
├── src/test/                             # Unit tests (XpCurve, resolvers, config)
└── docs/                                 # Phase documentation
```

## Phase Summary
| Phase | Goal                           | Depends On | Deliverable                                                  |
|-------|--------------------------------|------------|--------------------------------------------------------------|
| 0     | Project setup + hello world    | Nothing    | Compiling plugin, `/skills` command works                    | **Done** |
| 1     | Skill data model + persistence | Phase 0    | SkillType enum, PlayerSkillsComponent, save/load             | **Done** |
| 1.5   | Configuration system           | Phase 1    | SkillsConfig, config.json, tunable values available          | **Done** |
| 2     | XP gain + leveling             | Phase 1.5  | Gain XP from actions, level up with notifications            | **Done** |
| 3     | Skill effects                  | Phase 2    | Damage/stamina bonuses applied based on skill level          | **Done** |
| 4     | Death penalty                  | Phase 3    | Skill loss on death, immunity period                         | Not started |
| 5     | Skills UI                      | Phase 4    | `/skills` opens menu showing all skills + levels             | Not started |
| 6     | Polish + admin + release       | Phase 5    | Admin commands, performance optimization, CurseForge release | Not started |

## Key Constraints
- **Performance**: Skill checks happen frequently (every attack, every movement tick) — must be efficient
- **UI latency**: Skills UI is server round-trip; use `BasicCustomUIPage`/`InteractiveCustomUIPage` with `.ui` template files
- **Balance**: XP rates and bonuses need tuning; all values exposed in config
- **Thread safety**: ECS component access must happen on world thread; use `AbstractPlayerCommand` for commands, `world.execute()` for async→sync bridging
- **DamageModule dependency**: `DamageEvent` and `DeathEvent` require `"Hytale:DamageModule"` in manifest dependencies

## Validated APIs
The following APIs have been confirmed through implementation and SDK research:
- **ECS**: `Component<EntityStore>`, `ComponentType`, `putComponent()` (persistent), `addComponent()` (temporary), `BuilderCodec<T>`, `CommandBuffer<EntityStore>`
- **Events**: `PlayerReadyEvent`, `PlayerDisconnectEvent`, `DamageBlockEvent`, `DamageEvent`, `DeathEvent`, `BreakBlockEvent`
- **Event registration**: `getEventRegistry().register()` for regular events, `EntityEventSystem<EntityStore, Event>` for ECS events
- **Commands**: `CommandBase`, `AbstractPlayerCommand` (thread-safe ECS access), `AbstractCommandCollection` (subcommands), `ArgTypes`
- **ECS systems**: `EntityEventSystem<S,E>` for entity events, `EntityTickingSystem<S>` for tick systems, custom `DamageEventSystem` subclasses for damage pipeline — all registered via `entityStoreRegistry.registerSystem()`
- **UI**: `BasicCustomUIPage`, `InteractiveCustomUIPage`, `UICommandBuilder`, `UIEventBuilder`, `.ui` BSON layout files, `HudManager`
- **Damage pipeline**: `DamageModule` groups: `gatherDamageGroup` → `filterDamageGroup` → `inspectDamageGroup` → `ApplyDamage`; custom `DamageEventSystem` subclasses with `before(ApplyDamage)` / `after(ApplyDamage)` ordering
- **Damage MetaKeys**: `Damage.BLOCKED` (`MetaKey<Boolean>`), `Damage.STAMINA_DRAIN_MULTIPLIER` (`MetaKey<Float>`), `Damage.HIT_LOCATION`, `Damage.HIT_ANGLE`, `Damage.KNOCKBACK_COMPONENT`
- **Damage object**: `getAmount()`/`setAmount(float)`, `getMetaObject()`/`putMetaObject()`, `getSource()` → `Damage.Source`/`Damage.EntitySource`
- **Blocking**: All-or-nothing — `WieldingInteraction.DamageModifiers` are all 0 (100% blocked). Real cost is stamina: `StaminaCost { Value: 7, CostType: "Damage" }` → `damage / 7` stamina per block. Guard break on stamina depletion. Plugin reduces blocking stamina cost via `Damage.STAMINA_DRAIN_MULTIPLIER` MetaKey

## Configuration (config.json)
Managed via `BuilderCodec<T>` + `Config<T>` (native Hypixel API). Loaded with defaults via `loadConfig("skills", SkillsConfigCodec.CODEC)`.
```kotlin
SkillsConfig(
    general = GeneralConfig(maxLevel = 100, showLevelUpNotifications = true, showXpGainNotifications = false),
    xp = XpConfig(baseXpPerAction = 1.0, xpScaleFactor = 1.0, restedBonusMultiplier = 1.5, globalXpMultiplier = 1.0,
        actionXp = ActionXpConfig(
            combatDamageMultiplier = 0.1,
            miningPerBlockMultiplier = 1.0,
            woodcuttingPerBlockMultiplier = 1.0,
            runningPerDistanceMultiplier = 0.1,
            swimmingPerDistanceMultiplier = 0.1,
            sneakingPerSecondMultiplier = 0.1,
            jumpingPerJumpMultiplier = 0.5,
            blockingDamageMultiplier = 0.05,
            divingPerSecondMultiplier = 0.1
        )
    ),
    deathPenalty = DeathPenaltyConfig(enabled = true, penaltyPercentage = 0.1, immunityDurationSeconds = 300, showImmunityInHud = true),
    skillEffects = mapOf(/* per-skill SkillEffectEntry with damage, speed, stamina, jumpHeight */)
)
```

## Community Resources
- HytaleModding.dev — guides on plugins, ECS, events
- HytaleModding Discord — active modding community
- Valheim Wiki — reference implementation details
