# hytale-skills-plugin

A Hytale server plugin implementing a Valheim-inspired skill progression system. Players improve skills by performing actions (combat, gathering, movement). Higher skill levels provide gameplay bonuses. Death incurs a configurable skill penalty.

## Skills

15 skills across 3 categories:

| Category | Skills |
|----------|--------|
| Combat | Swords, Daggers, Axes, Bows, Spears, Clubs, Unarmed, Blocking |
| Gathering | Mining, Woodcutting |
| Movement  | Running, Swimming, Diving, Sneaking, Jumping |

All skills range from level 0 to 100. XP is gained by performing the associated action.

## Architecture

```
org.zunkree.hytale.plugins.skillsplugin/
    SkillsPlugin.kt              # Entry point & composition root
    HexweaveRegistration.kt      # Hexweave system wiring (extension function)
    config/
        SkillsConfig.kt          # All configuration data classes
        SkillsConfigValidator.kt # Startup config validation
    skill/
        SkillType.kt             # Skill enum with categories and display names
        SkillCategory.kt         # COMBAT, GATHERING, MOVEMENT
        SkillData.kt             # Per-skill level + XP data
        PlayerSkillsComponent.kt # ECS component holding all player skills
    xp/
        XpCurve.kt               # XP formulas: requirements, cumulative, level progress
        XpService.kt             # Grants XP, handles level-ups, death penalty
    effect/
        SkillEffectCalculator.kt # Config-driven linear interpolation for bonuses
        CombatEffectApplier.kt   # Damage & blocking stamina modifiers
        MovementEffectApplier.kt # Speed & jump force modifiers (per-player state)
        StatEffectApplier.kt     # Stamina & oxygen stat modifiers
        GatheringEffectApplier.kt # Mining/woodcutting speed modifiers
    resolver/
        WeaponSkillResolver.kt   # Item ID → combat SkillType (pure)
        BlockSkillResolver.kt    # Block ID → gathering SkillType (pure)
        MovementXpPolicy.kt      # Movement state → XP grants (pure)
    listener/
        CombatListener.kt        # Damage events → combat XP
        HarvestListener.kt       # Block break events → gathering XP
        MovementListener.kt      # Movement ticks → movement XP
        BlockingListener.kt      # Block events → blocking XP
        PlayerLifecycleListener.kt # Join/leave → load/save skills
    command/
        SkillsCommandHandler.kt  # /skills command
    persistence/
        SkillRepository.kt       # ECS read/write for skill components
```

### Design Principles

- **Constructor injection**: No singletons or global state. `SkillsPlugin.setup()` is the sole composition root.
- **Pure domain logic**: Resolvers and policies have zero framework imports — fully testable without mocking.
- **Thin listeners**: Event listeners parse framework events and delegate to resolvers/policies.
- **ECS persistence**: Skills stored via `putComponent()` for server-restart survival.

## Development

### Prerequisites

- Java 25
- Hytale installed via official launcher
- Kytale dependency (copied to `build/libs` by Gradle)

### Build & Run

```bash
# Build the plugin JAR
./gradlew build

# Run the server
./gradlew run

# Run tests
./gradlew test

# Run all quality checks (lint + static analysis + tests)
./gradlew ktlintCheck detekt test

# Auto-fix formatting issues
./gradlew ktlintFormat

# Check code coverage
./gradlew koverVerify
```

### Quality Gates

| Tool | Purpose | Config |
|------|---------|--------|
| [ktlint](https://pinterest.github.io/ktlint/) | Kotlin code formatting | Default rules, max line length 120 |
| [detekt](https://detekt.dev/) | Static analysis | `detekt.yml` — MagicNumber disabled, MaxLineLength=120 |
| [Kover](https://github.com/Kotlin/kotlinx-kover) | Code coverage | 50% minimum (framework code is untestable) |
| JUnit 5 | Unit tests | `src/test/kotlin/` |

### CI/CD

GitLab CI pipeline (`.gitlab-ci.yml`) runs two jobs:
- **quality**: `ktlintCheck` + `detekt`
- **test**: `test` + `koverVerify`

No `build` job in CI — building requires a local Hytale installation.

## Configuration

The plugin reads `config.json` with sections for general settings, XP curve tuning, per-action XP values, death penalty, and skill effects. All values are validated at startup with clear error messages.

Key tunable values:
- `maxLevel` — skill cap (default: 100)
- `baseXpPerAction` / `xpScaleFactor` — XP curve shape
- `globalXpMultiplier` / `restedBonusMultiplier` — XP rate modifiers
- `deathPenaltyPercentage` / `deathImmunityDuration` — death penalty tuning
- Per-skill effect entries (damage multipliers, speed bonuses, stamina reduction)

## Adding a New Skill

See [docs/guide-adding-a-skill.md](docs/guide-adding-a-skill.md) for a step-by-step guide.

## Documentation

- [Project Overview](docs/00-PROJECT-OVERVIEW.md) — Vision, reference mechanics, phase roadmap
- [ADR-001: Persistence Key Strategy](docs/adr/001-persistence-keys.md)
- [ADR-002: Dependency Injection Strategy](docs/adr/002-di-strategy.md)
