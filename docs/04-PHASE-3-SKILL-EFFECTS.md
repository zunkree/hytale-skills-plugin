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

### Task 3.1 — Create `SkillEffectCalculator.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.config.SkillEffectEntry

class SkillEffectCalculator(
    private val skillEffectsConfig: Map<String, SkillEffectEntry>,
    private val maxLevel: Int
) {
    /**
     * Linear interpolation: bonus = min + (max - min) * (level / maxLevel)
     */
    fun getDamageMultiplier(skillType: SkillType, level: Int): Double {
        val entry = skillEffectsConfig[skillType.name] ?: return 1.0
        val min = entry.minDamage?.toDouble() ?: 1.0
        val max = entry.maxDamage?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getBlockPowerMultiplier(level: Int): Double {
        val entry = skillEffectsConfig["BLOCKING"] ?: return 1.0
        val min = entry.minBlockPower?.toDouble() ?: 1.0
        val max = entry.maxBlockPower?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getGatheringSpeedMultiplier(skillType: SkillType, level: Int): Double {
        val entry = skillEffectsConfig[skillType.name] ?: return 1.0
        val min = entry.minSpeed?.toDouble() ?: 1.0
        val max = entry.maxSpeed?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getSpeedBonus(level: Int): Double {
        val entry = skillEffectsConfig["RUNNING"] ?: return 0.0
        val min = entry.minSpeedBonus?.toDouble() ?: 0.0
        val max = entry.maxSpeedBonus?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    fun getStaminaReduction(skillType: SkillType, level: Int): Double {
        val entry = skillEffectsConfig[skillType.name] ?: return 0.0
        val min = entry.minStaminaReduction?.toDouble() ?: 0.0
        val max = entry.maxStaminaReduction?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    fun getJumpHeightMultiplier(level: Int): Double {
        val entry = skillEffectsConfig["JUMPING"] ?: return 1.0
        val min = entry.minHeight?.toDouble() ?: 1.0
        val max = entry.maxHeight?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    private fun lerp(min: Double, max: Double, level: Int): Double {
        val progress = level.toDouble() / maxLevel
        return min + (max - min) * progress
    }
}
```

> **Architecture:** `SkillEffectCalculator` is a class (not an object) that takes config via constructor injection. Instantiated once in `SkillsPlugin.setup()`. All values are `Double` for consistency with the rest of the plugin.

### Task 3.2 — Create `SkillEffectApplier.kt`

Unified service that applies skill effects via Hexweave systems:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

class SkillEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger
) {
    // Called from Hexweave damage system (before ApplyDamage)
    fun modifyOutgoingDamage(ctx: HexweaveDamageContext) {
        val playerRef = ctx.source?.playerRef ?: return
        val itemId = ctx.source?.itemInHand?.id ?: ""
        val skillType = weaponSkillResolver.resolve(itemId) ?: SkillType.UNARMED
        val level = skillRepository.getSkillLevel(playerRef, skillType)
        val multiplier = effectCalculator.getDamageMultiplier(skillType, level)
        ctx.damage *= multiplier
    }

    // Called from Hexweave damage system (before ApplyDamage, target side)
    fun modifyBlockPower(ctx: HexweaveDamageContext) {
        val playerRef = ctx.target?.playerRef ?: return
        val level = skillRepository.getSkillLevel(playerRef, SkillType.BLOCKING)
        val multiplier = effectCalculator.getBlockPowerMultiplier(level)
        ctx.blockPower = (ctx.blockPower ?: 0.0) * multiplier
    }

    // Called from Hexweave tick system — modifies player attributes each tick
    fun applyMovementEffects(ctx: HexweaveTickContext) {
        val playerRef = ctx.playerRef

        // Running speed bonus
        val runLevel = skillRepository.getSkillLevel(playerRef, SkillType.RUNNING)
        val speedBonus = effectCalculator.getSpeedBonus(runLevel)
        ctx.sprintSpeedMultiplier = 1.0 + speedBonus

        // Jump height bonus
        val jumpLevel = skillRepository.getSkillLevel(playerRef, SkillType.JUMPING)
        ctx.jumpHeightMultiplier = effectCalculator.getJumpHeightMultiplier(jumpLevel)
    }

    // Called from Hexweave tick system — modifies stamina drain
    fun applyStaminaEffects(ctx: HexweaveTickContext) {
        val playerRef = ctx.playerRef

        if (ctx.isSprinting) {
            val level = skillRepository.getSkillLevel(playerRef, SkillType.RUNNING)
            val reduction = effectCalculator.getStaminaReduction(SkillType.RUNNING, level)
            ctx.staminaDrainMultiplier *= (1.0 - reduction)
        }
        if (ctx.isSwimming) {
            val level = skillRepository.getSkillLevel(playerRef, SkillType.SWIMMING)
            val reduction = effectCalculator.getStaminaReduction(SkillType.SWIMMING, level)
            ctx.staminaDrainMultiplier *= (1.0 - reduction)
        }
        if (ctx.isSneaking) {
            val level = skillRepository.getSkillLevel(playerRef, SkillType.SNEAKING)
            val reduction = effectCalculator.getStaminaReduction(SkillType.SNEAKING, level)
            ctx.staminaDrainMultiplier *= (1.0 - reduction)
        }
    }
}
```

> **Architecture:** `SkillEffectApplier` is a single class (not multiple `object` singletons) that applies all skill effects. Uses constructor injection for dependencies. Hooks into Hexweave damage and tick systems.

### Task 3.3 — Create `GatheringEffectApplier.kt`

Modifies gathering speed via DamageBlockEvent:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

class GatheringEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger
) {
    fun modifyBlockDamage(ctx: EntityEventContext<EntityStore, DamageBlockEvent>) {
        val playerRef = ctx.source?.playerRef ?: return
        val blockId = ctx.event.blockType.id
        val skillType = blockSkillResolver.resolve(blockId) ?: return
        val level = skillRepository.getSkillLevel(playerRef, skillType)
        val multiplier = effectCalculator.getGatheringSpeedMultiplier(skillType, level)
        ctx.event.damage *= multiplier
    }
}
```

### Task 3.4 — Register effect systems in plugin setup

```kotlin
override fun setup() {
    super.setup()
    // ... existing code ...

    val effectCalculator = SkillEffectCalculator(config.skillEffects, config.general.maxSkillLevel)
    val effectApplier = SkillEffectApplier(skillRepository, effectCalculator, weaponSkillResolver, logger)
    val gatheringEffectApplier = GatheringEffectApplier(skillRepository, effectCalculator, blockSkillResolver, logger)

    enableHexweave {
        damageSystems {
            // Effects: modify damage BEFORE it's applied
            before(DamageSystems.ApplyDamage) { ctx ->
                effectApplier.modifyOutgoingDamage(ctx)
                effectApplier.modifyBlockPower(ctx)
            }
            // XP: grant XP AFTER damage is applied (from Phase 2)
            after(DamageSystems.ApplyDamage) { ctx ->
                combatListener.onPlayerDealDamage(ctx)
                blockingListener.onPlayerBlockDamage(ctx)
            }
        }
        tickSystems {
            playerTick { ctx ->
                effectApplier.applyMovementEffects(ctx)
                effectApplier.applyStaminaEffects(ctx)
                movementListener.onPlayerTick(ctx)
            }
        }
    }

    // Gathering effects via ECS event
    EntityEventSystem<EntityStore, DamageBlockEvent> { ctx ->
        gatheringEffectApplier.modifyBlockDamage(ctx)
        harvestListener.onPlayerDamageBlock(ctx)
    }
}
```

> **Note:** Effects use `before(DamageSystems.ApplyDamage)` to modify values before damage is applied, while XP listeners use `after(DamageSystems.ApplyDamage)` to grant XP based on actual damage dealt.

---

## Effect Summary

| Skill | Effect at Level 0 | Effect at Level 100 |
|-------|-------------------|---------------------|
| Swords | 1.0x damage | 2.0x damage |
| Daggers | 1.0x damage | 2.0x damage |
| Axes | 1.0x damage | 2.0x damage |
| Bows | 1.0x damage | 2.0x damage |
| Spears | 1.0x damage | 2.0x damage |
| Clubs | 1.0x damage | 2.0x damage |
| Unarmed | 1.0x damage | 2.0x damage |
| Blocking | 1.0x block power | 1.5x block power |
| Mining | 1.0x speed | 1.5x speed |
| Woodcutting | 1.0x speed | 1.5x speed |
| Running | +0% speed, 0% stamina reduction | +25% speed, 33% stamina reduction |
| Swimming | 0% stamina reduction | 50% stamina reduction |
| Diving | TBD (oxygen capacity) | TBD |
| Sneaking | 0% stamina reduction | 75% stamina reduction |
| Jumping | 1.0x height | 1.5x height |

---

## Validated APIs

- [x] **Hexweave damage system** — `before(DamageSystems.ApplyDamage)` to modify damage/block power before application
- [x] **Hexweave tick system** — `playerTick { ctx -> }` to modify movement speed, jump height, stamina drain per tick
- [x] **DamageBlockEvent** — ECS event to modify gathering speed (block damage multiplier)
- [x] **CommandBuffer** — Available in all Hexweave and EntityEvent contexts for batched ECS writes
- [x] **WeaponSkillResolver** — Reused from Phase 2 for weapon skill type detection in damage modifier

### Research Still Needed

- [ ] **Stamina system** — Exact API for modifying stamina drain rates (ctx.staminaDrainMultiplier is speculative)
- [ ] **Diving/oxygen** — How to detect submerged state and modify oxygen capacity

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Effects not applying | Verify event hooks are correct |
| Wrong multiplier values | Check config and level calculation |
| Performance issues | Cache skill levels, don't query on every frame |
| Effects too strong/weak | Adjust config values |
