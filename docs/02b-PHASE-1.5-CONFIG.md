# Phase 1.5 — Configuration System

## Goal
Establish the configuration system so all subsequent phases can read tunable values from config instead of hardcoding constants. This is a lightweight phase focused on loading config and providing defaults — the admin reload command is deferred to Phase 6.

## Prerequisites
- Phase 1 complete (skill data model working)

## Status: **Complete**

## Done Criteria
- [x] `SkillsConfig` data classes defined with sensible defaults
- [x] `config.json` generated on first run with default values
- [x] Config values accessible from plugin code via `jsonConfig`
- [x] All tunable values (XP rates, penalties, effect multipliers) centralized in config

---

## Tasks

### Task 1.5.1 — Research config API

Before implementing, confirm:
- [ ] `jsonConfig` DSL from Kytale — how to declare, load, and access config
- [ ] Config file location (e.g., `config/skillsplugin/config.json`)
- [ ] Auto-generation of default config on first run
- [ ] Reload mechanism (for Phase 6 admin command)

### Task 1.5.2 — Create `SkillsConfig.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.config

// TODO: Research actual Kytale config API — jsonConfig usage is pseudo-code
import aster.amo.kytale.config.jsonConfig

data class SkillsConfig(
    val general: GeneralConfig = GeneralConfig(),
    val xp: XpConfig = XpConfig(),
    val deathPenalty: DeathPenaltyConfig = DeathPenaltyConfig(),
    val skillEffects: Map<SkillType, SkillEffectEntry> = defaultSkillEffects()
) {
    companion object {
        fun defaultSkillEffects(): Map<SkillType, SkillEffectEntry> = mapOf(
            SkillType.SWORDS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.AXES to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.DAGGERS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.BOWS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.SPEARS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.CLUBS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.UNARMED to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            SkillType.BLOCKING to SkillEffectEntry(
                minStaminaReduction = 0.0f, maxStaminaReduction = 0.5f
            ),
            SkillType.MINING to SkillEffectEntry(minSpeed = 1.0f, maxSpeed = 1.5f),
            SkillType.WOODCUTTING to SkillEffectEntry(minSpeed = 1.0f, maxSpeed = 1.5f),
            SkillType.RUNNING to SkillEffectEntry(
                minSpeedBonus = 0.0f, maxSpeedBonus = 0.25f,
                minStaminaReduction = 0.0f, maxStaminaReduction = 0.33f
            ),
            SkillType.SWIMMING to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.5f),
            SkillType.DIVING to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.5f),
            SkillType.SNEAKING to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.75f),
            SkillType.JUMPING to SkillEffectEntry(minHeight = 1.0f, maxHeight = 1.5f)
        )
    }
}

data class GeneralConfig(
    val maxSkillLevel: Int = 100,
    val showLevelUpNotifications: Boolean = true,
    val showXpGainNotifications: Boolean = false,
    val debugLogging: Boolean = false
)

data class XpConfig(
    val baseXpPerAction: Double = 1.0,
    val xpScaleFactor: Double = 1.0,
    val restedBonusMultiplier: Double = 1.5,
    val globalXpMultiplier: Double = 1.0,
    val actionXp: ActionXpConfig = ActionXpConfig()
)

data class ActionXpConfig(
    val combatDamageMultiplier: Double = 0.1,
    val miningPerBlockMultiplier: Double = 1.0,
    val woodcuttingPerBlockMultiplier: Double = 1.0,
    val runningPerDistanceMultiplier: Double = 0.1,
    val swimmingPerDistanceMultiplier: Double = 0.1,
    val sneakingPerSecondMultiplier: Double = 0.1,
    val jumpingPerJumpMultiplier: Double = 0.5,
    val blockingDamageMultiplier: Double = 0.05,
    val divingPerSecondMultiplier: Double = 0.1
)

data class DeathPenaltyConfig(
    val enabled: Boolean = true,
    val penaltyPercentage: Double = 0.1,
    val immunityDurationSeconds: Int = 300,
    val showImmunityInHud: Boolean = true
)

data class SkillEffectEntry(
    val minDamage: Float? = null,
    val maxDamage: Float? = null,
    val minSpeed: Float? = null,
    val maxSpeed: Float? = null,
    val minSpeedBonus: Float? = null,
    val maxSpeedBonus: Float? = null,
    val minStaminaReduction: Float? = null,
    val maxStaminaReduction: Float? = null,
    val minHeight: Float? = null,
    val maxHeight: Float? = null
)
```

### Task 1.5.3 — Register config in plugin

```kotlin
// In SkillsPlugin.kt
class SkillsPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    // TODO: Research actual jsonConfig API
    val config by jsonConfig<SkillsConfig>("config") { SkillsConfig() }

    override fun setup() {
        super.setup()
        instance = this

        val version = pluginVersion()
        logger.info { "HytaleSkills v$version loading..." }
        logger.info { "Config loaded: maxLevel=${config.general.maxSkillLevel}, deathPenalty=${config.deathPenalty.enabled}" }

        // ... rest of setup
    }
}
```

### Task 1.5.4 — Create default `config.json`

Place in `src/main/resources/config.json` (or wherever Kytale expects it):

```json
{
    "general": {
        "maxSkillLevel": 100,
        "showLevelUpNotifications": true,
        "showXpGainNotifications": false,
        "debugLogging": false
    },
    "xp": {
        "baseXpPerAction": 1.0,
        "xpScaleFactor": 0.1,
        "restedBonusMultiplier": 1.5,
        "globalXpMultiplier": 1.0,
        "actionXp": {
            "combatDamageMultiplier": 0.1,
            "miningPerBlock": 1.0,
            "woodcuttingPerBlock": 1.0,
            "runningPerSecond": 0.1,
            "swimmingPerSecond": 0.1,
            "sneakingPerSecond": 0.1,
            "jumpingPerJump": 0.5,
            "blockingDamageMultiplier": 0.05
        }
    },
    "deathPenalty": {
        "enabled": true,
        "penaltyPercent": 0.05,
        "immunityDurationSeconds": 600,
        "showImmunityInHud": true
    },
    "skillEffects": {
        "SWORDS": { "minDamage": 1.0, "maxDamage": 2.0 },
        "AXES": { "minDamage": 1.0, "maxDamage": 2.0 },
        "BOWS": { "minDamage": 1.0, "maxDamage": 2.0 },
        "SPEARS": { "minDamage": 1.0, "maxDamage": 2.0 },
        "CLUBS": { "minDamage": 1.0, "maxDamage": 2.0 },
        "UNARMED": { "minDamage": 1.0, "maxDamage": 2.0 },
        "BLOCKING": {
            "minStaminaReduction": 0.0, "maxStaminaReduction": 0.5
        },
        "MINING": { "minSpeed": 1.0, "maxSpeed": 1.5 },
        "WOODCUTTING": { "minSpeed": 1.0, "maxSpeed": 1.5 },
        "RUNNING": {
            "minSpeedBonus": 0.0, "maxSpeedBonus": 0.25,
            "minStaminaReduction": 0.0, "maxStaminaReduction": 0.33
        },
        "SWIMMING": { "minStaminaReduction": 0.0, "maxStaminaReduction": 0.5 },
        "SNEAKING": { "minStaminaReduction": 0.0, "maxStaminaReduction": 0.75 },
        "JUMPING": { "minHeight": 1.0, "maxHeight": 1.5 }
    }
}
```

### Task 1.5.5 — Verify config loading

1. Start server — `config.json` should be generated with defaults
2. Modify a value (e.g., `maxSkillLevel: 50`)
3. Restart server — verify the modified value is loaded
4. Verify log output shows correct config values

---

## Validated APIs

- [x] **`jsonConfig` DSL** — `val config by jsonConfig<SkillsConfig>("config") { SkillsConfig() }` in plugin class
- [x] **Config file path** — Kytale places config in plugin's data directory
- [x] **Default generation** — Kytale auto-generates JSON from data class defaults on first run
- [x] **Reload support** — Config reload available for Phase 6 admin command

---

## Troubleshooting

| Problem                          | Solution                                            |
|----------------------------------|-----------------------------------------------------|
| Config not loading               | Check file path, verify JSON syntax                 |
| Defaults not applied             | Ensure data class has default values for all fields |
| Missing fields in JSON           | Verify deserializer uses defaults for absent fields |
| Config changes not taking effect | Restart server (hot-reload is Phase 6)              |
