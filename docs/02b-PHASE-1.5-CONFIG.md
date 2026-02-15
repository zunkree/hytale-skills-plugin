# Phase 1.5 — Configuration System

## Goal
Establish the configuration system so all subsequent phases can read tunable values from config instead of hardcoding constants. This is a lightweight phase focused on loading config and providing defaults — the admin reload command is deferred to Phase 6.

## Prerequisites
- Phase 1 complete (skill data model working)

## Status: **Complete**

## Done Criteria
- [x] `SkillsConfig` data classes defined with sensible defaults
- [x] `config.json` generated on first run with default values
- [x] Config values accessible from plugin code via `BuilderCodec<T>` + `Config<T>`
- [x] All tunable values (XP rates, penalties, effect multipliers) centralized in config

---

## Tasks

### Task 1.5.1 — Research config API

Before implementing, confirm:
- [x] `BuilderCodec<T>` — how to define codecs for config data classes
- [x] `JavaPlugin.withConfig(name, codec)` — returns `Config<T>` handle
- [x] `Config<T>.load()` — returns `CompletableFuture<T>` with loaded/default values
- [x] Config file location (plugin's data directory)
- [x] Reload mechanism — `Config<T>.load()` re-reads from disk (for Phase 6 admin command)

### Task 1.5.2 — Create `SkillsConfig.kt`

All config fields use `var` (not `val`) because `BuilderCodec` mutates objects via setters.

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.config

data class SkillsConfig(
    var general: GeneralConfig = GeneralConfig(),
    var xp: XpConfig = XpConfig(),
    var deathPenalty: DeathPenaltyConfig = DeathPenaltyConfig(),
    var skillEffects: Map<SkillType, SkillEffectEntry> = defaultSkillEffects(),
) {
    companion object {
        fun defaultSkillEffects(): Map<SkillType, SkillEffectEntry> = mapOf(
            SkillType.SWORDS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            // ... all skills ...
        )
    }
}

data class GeneralConfig(
    var maxLevel: Int = 100,
    var showLevelUpNotifications: Boolean = true,
    var showXpGainNotifications: Boolean = false,
)

data class XpConfig(
    var baseXpPerAction: Double = 1.0,
    var xpScaleFactor: Double = 1.0,
    var restedBonusMultiplier: Double = 1.5,
    var globalXpMultiplier: Double = 1.0,
    var actionXp: ActionXpConfig = ActionXpConfig(),
)

data class ActionXpConfig(
    var combatDamageMultiplier: Double = 0.1,
    var miningPerBlockMultiplier: Double = 1.0,
    var woodcuttingPerBlockMultiplier: Double = 1.0,
    var runningPerDistanceMultiplier: Double = 0.1,
    var swimmingPerDistanceMultiplier: Double = 0.1,
    var sneakingPerSecondMultiplier: Double = 0.1,
    var jumpingPerJumpMultiplier: Double = 0.5,
    var blockingDamageMultiplier: Double = 0.05,
    var divingPerSecondMultiplier: Double = 0.1,
)

data class DeathPenaltyConfig(
    var enabled: Boolean = true,
    var penaltyPercentage: Double = 0.1,
    var immunityDurationSeconds: Int = 300,
    var showImmunityInHud: Boolean = true,
)

data class SkillEffectEntry(
    var minDamage: Float? = null,
    var maxDamage: Float? = null,
    var minSpeed: Float? = null,
    var maxSpeed: Float? = null,
    var minSpeedBonus: Float? = null,
    var maxSpeedBonus: Float? = null,
    var minStaminaReduction: Float? = null,
    var maxStaminaReduction: Float? = null,
    var minHeight: Float? = null,
    var maxHeight: Float? = null,
    var minOxygenBonus: Float? = null,
    var maxOxygenBonus: Float? = null,
)
```

> **Note:** Fields are `var` (not `val`) because `BuilderCodec` populates objects by calling setters on a default-constructed instance.

### Task 1.5.3 — Register config in plugin

Config is loaded in `PluginApplication.setup()` using the native `Config<T>` API:

```kotlin
// In PluginApplication.kt
fun setup() {
    val configRef = plugin.loadConfig("skills", SkillsConfigCodec.CODEC)
    val config: SkillsConfig = configRef.load().join()
    SkillsConfigValidator.validate(config)
    // ... use config for service creation ...
}

// In SkillsPlugin.kt
class SkillsPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    fun <T : Any> loadConfig(name: String, codec: BuilderCodec<T>): Config<T> =
        withConfig(name, codec)
}
```

A separate `SkillsConfigCodec` object builds the `BuilderCodec<SkillsConfig>` by composing sub-codecs for each config section. Each codec field maps a JSON key name to a getter/setter pair:

```kotlin
object SkillsConfigCodec {
    val CODEC: BuilderCodec<SkillsConfig> = run {
        val builder = BuilderCodec.builder(SkillsConfig::class.java, ::SkillsConfig)
            .append(KeyedCodec("General", GENERAL_CODEC), { obj, v -> obj.general = v }, { it.general }).add()
            .append(KeyedCodec("Xp", XP_CODEC), { obj, v -> obj.xp = v }, { it.xp }).add()
            .append(KeyedCodec("DeathPenalty", DEATH_PENALTY_CODEC), { obj, v -> obj.deathPenalty = v }, { it.deathPenalty }).add()
        // ... skill effect entries ...
        builder.build()
    }
}
```

### Task 1.5.4 — Create default `config.json`

Place in the plugin's data directory (auto-generated from `BuilderCodec` defaults):

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

- [x] **`BuilderCodec<T>`** — `BuilderCodec.builder(Class, Supplier).append(KeyedCodec(...), setter, getter).add().build()` for config serialization
- [x] **`Config<T>`** — `JavaPlugin.withConfig(name, codec)` returns a `Config<T>` handle
- [x] **`Config<T>.load()`** — Returns `CompletableFuture<T>` with loaded/default values
- [x] **Config file path** — Native API places config in plugin's data directory
- [x] **Reload support** — `Config<T>.load()` re-reads from disk (for Phase 6 admin command)

---

## Troubleshooting

| Problem                          | Solution                                            |
|----------------------------------|-----------------------------------------------------|
| Config not loading               | Check file path, verify JSON syntax                 |
| Defaults not applied             | Ensure data class has default values for all fields |
| Missing fields in JSON           | Verify deserializer uses defaults for absent fields |
| Config changes not taking effect | Restart server (hot-reload is Phase 6)              |
