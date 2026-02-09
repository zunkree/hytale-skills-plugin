# Phase 3 — Skill Effects

## Goal
Apply gameplay bonuses based on skill levels. Higher skill levels provide tangible benefits: increased damage, reduced stamina usage, faster actions, etc.

## Prerequisites
- Phase 2 complete (XP gain and leveling working)

## Done Criteria
- [ ] Weapon skills increase damage dealt (up to 2x at level 100)
- [ ] Blocking skill increases block power (up to 1.5x at level 100)
- [ ] Running skill increases speed and reduces stamina drain
- [ ] Swimming skill reduces stamina drain
- [ ] Sneaking skill reduces detection and stamina drain
- [ ] Jumping skill increases jump height
- [ ] Mining/Woodcutting skills increase gathering speed
- [ ] All effects scale linearly from level 0 to 100

---

## Tasks

### Task 3.1 — Create `SkillEffectConfig.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

data class SkillEffectConfig(
    val minMultiplier: Float,
    val maxMultiplier: Float,
    val minStaminaReduction: Float = 0f,
    val maxStaminaReduction: Float = 0f,
    val minSpeedBonus: Float = 0f,
    val maxSpeedBonus: Float = 0f
) {
    fun getMultiplierAtLevel(level: Int): Float {
        val progress = level / 100f
        return minMultiplier + (maxMultiplier - minMultiplier) * progress
    }

    fun getStaminaReductionAtLevel(level: Int): Float {
        val progress = level / 100f
        return minStaminaReduction + (maxStaminaReduction - minStaminaReduction) * progress
    }

    fun getSpeedBonusAtLevel(level: Int): Float {
        val progress = level / 100f
        return minSpeedBonus + (maxSpeedBonus - minSpeedBonus) * progress
    }
}

object SkillEffects {

    /**
     * Build effect configs from plugin config (Phase 1.5).
     * Falls back to sensible defaults if a skill is missing from config.
     */
    fun getConfig(skillType: SkillType): SkillEffectConfig {
        val pluginConfig = HytaleSkillsPlugin.instance.config
        val entry = pluginConfig.skillEffects[skillType.name]

        return if (entry != null) {
            SkillEffectConfig(
                minMultiplier = entry.minDamage ?: entry.minBlockPower ?: entry.minSpeed ?: entry.minHeight ?: 1.0f,
                maxMultiplier = entry.maxDamage ?: entry.maxBlockPower ?: entry.maxSpeed ?: entry.maxHeight ?: 1.0f,
                minSpeedBonus = entry.minSpeedBonus ?: 0f,
                maxSpeedBonus = entry.maxSpeedBonus ?: 0f,
                minStaminaReduction = entry.minStaminaReduction ?: 0f,
                maxStaminaReduction = entry.maxStaminaReduction ?: 0f
            )
        } else {
            SkillEffectConfig(1.0f, 1.0f) // No effect
        }
    }
}
```

### Task 3.2 — Create `DamageModifier.kt`

Modify outgoing damage based on weapon skill:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillManager
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

object DamageModifier {

    fun modifyDamage(
        playerRef: /* PlayerRef */,
        baseDamage: Float,
        weaponSkillType: SkillType
    ): Float {
        val skillLevel = SkillManager.getSkillLevel(playerRef, weaponSkillType)
        val config = SkillEffects.getConfig(weaponSkillType)
        val multiplier = config.getMultiplierAtLevel(skillLevel)
        return baseDamage * multiplier
    }

    fun modifyBlockPower(
        playerRef: /* PlayerRef */,
        baseBlockPower: Float
    ): Float {
        val skillLevel = SkillManager.getSkillLevel(playerRef, SkillType.BLOCKING)
        val config = SkillEffects.getConfig(SkillType.BLOCKING)
        val multiplier = config.getMultiplierAtLevel(skillLevel)
        return baseBlockPower * multiplier
    }
}
```

### Task 3.3 — Create `StaminaModifier.kt`

Modify stamina consumption based on skills:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillManager
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

object StaminaModifier {

    fun modifyStaminaCost(
        playerRef: /* PlayerRef */,
        baseStaminaCost: Float,
        action: StaminaAction
    ): Float {
        val skillType = action.relatedSkill
        val skillLevel = SkillManager.getSkillLevel(playerRef, skillType)
        val config = SkillEffects.getConfig(skillType)
        val reduction = config.getStaminaReductionAtLevel(skillLevel)
        return baseStaminaCost * (1f - reduction)
    }
}

enum class StaminaAction(val relatedSkill: SkillType) {
    RUNNING(SkillType.RUNNING),
    SWIMMING(SkillType.SWIMMING),
    SNEAKING(SkillType.SNEAKING)
}
```

### Task 3.4 — Create `MovementModifier.kt`

Modify movement speed and jump height:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillManager
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

object MovementModifier {

    fun getSprintSpeedMultiplier(playerRef: /* PlayerRef */): Float {
        val skillLevel = SkillManager.getSkillLevel(playerRef, SkillType.RUNNING)
        val config = SkillEffects.getConfig(SkillType.RUNNING)
        return 1f + config.getSpeedBonusAtLevel(skillLevel)
    }

    fun getJumpHeightMultiplier(playerRef: /* PlayerRef */): Float {
        val skillLevel = SkillManager.getSkillLevel(playerRef, SkillType.JUMPING)
        val config = SkillEffects.getConfig(SkillType.JUMPING)
        return config.getMultiplierAtLevel(skillLevel)
    }
}
```

### Task 3.5 — Create `GatheringModifier.kt`

Modify mining/woodcutting speed:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillManager
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

object GatheringModifier {

    fun getMiningSpeedMultiplier(playerRef: /* PlayerRef */): Float {
        val skillLevel = SkillManager.getSkillLevel(playerRef, SkillType.MINING)
        val config = SkillEffects.getConfig(SkillType.MINING)
        return config.getMultiplierAtLevel(skillLevel)
    }

    fun getWoodcuttingSpeedMultiplier(playerRef: /* PlayerRef */): Float {
        val skillLevel = SkillManager.getSkillLevel(playerRef, SkillType.WOODCUTTING)
        val config = SkillEffects.getConfig(SkillType.WOODCUTTING)
        return config.getMultiplierAtLevel(skillLevel)
    }
}
```

### Task 3.6 — Hook modifiers into game events

This is the most Hytale-API-dependent part. You need to find the correct events/hooks to:

1. **Modify outgoing damage** — Hook into damage calculation before it's applied
2. **Modify block power** — Hook into block/parry calculation
3. **Modify stamina drain** — Hook into stamina consumption
4. **Modify movement speed** — Hook into player movement/attributes
5. **Modify jump height** — Hook into jump calculation
6. **Modify gathering speed** — Hook into block break time calculation

Example pseudo-code for damage modification:

```kotlin
// TODO: All event types below are pseudo-code — research actual Hytale event/hook APIs
events {
    on<PreDamageCalculationEvent> { event ->
        if (event.attacker.isPlayer) {
            val playerRef = event.attacker.asPlayerRef()
            val weaponSkill = getWeaponSkillType(event.weapon)
            event.damage = DamageModifier.modifyDamage(
                playerRef,
                event.damage,
                weaponSkill
            )
        }
    }
}
```

> **Note:** All event types (`PreDamageCalculationEvent`, etc.) and hook patterns in this phase are pseudo-code representing design intent. The actual Hytale APIs for modifying damage calculations, stamina consumption, movement attributes, and gathering speed need to be discovered from SDK documentation.

---

## Effect Summary

| Skill | Effect at Level 0 | Effect at Level 100 |
|-------|-------------------|---------------------|
| Swords | 1.0x damage | 2.0x damage |
| Axes | 1.0x damage | 2.0x damage |
| Bows | 1.0x damage | 2.0x damage |
| Spears | 1.0x damage | 2.0x damage |
| Clubs | 1.0x damage | 2.0x damage |
| Unarmed | 1.0x damage | 2.0x damage |
| Blocking | 1.0x block power | 1.5x block power |
| Running | +0% speed, 0% stamina reduction | +25% speed, 33% stamina reduction |
| Swimming | 0% stamina reduction | 50% stamina reduction |
| Sneaking | 0% stamina reduction | 75% stamina reduction |
| Jumping | 1.0x height | 1.5x height |
| Mining | 1.0x speed | 1.5x speed |
| Woodcutting | 1.0x speed | 1.5x speed |

---

## Research Required

Before implementing this phase, confirm the following APIs against actual Hytale/Kytale SDK documentation:

- [ ] **Damage modification hooks** — How to intercept and modify outgoing damage before it's applied
- [ ] **Block power modification** — How to modify blocking/parry effectiveness
- [ ] **Stamina consumption hooks** — How to modify stamina drain for running/swimming/sneaking
- [ ] **Movement attribute modification** — How to modify sprint speed and jump height on a per-player basis
- [ ] **Gathering speed modification** — How to modify block break time/speed
- [ ] **Player attribute system** — Whether Hytale has an attribute modifier system (like Minecraft's) vs direct value modification

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Effects not applying | Verify event hooks are correct |
| Wrong multiplier values | Check config and level calculation |
| Performance issues | Cache skill levels, don't query on every frame |
| Effects too strong/weak | Adjust config values |
