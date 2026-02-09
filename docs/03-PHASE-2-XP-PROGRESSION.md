# Phase 2 — XP Gain & Leveling

## Goal
Implement XP gain from player actions. Players earn XP in relevant skills by performing actions: attacking with weapons, mining, chopping trees, running, swimming, etc.

## Prerequisites
- Phase 1.5 complete (skill data model, persistence, and config system working)

## Done Criteria
- [ ] Attacking enemies with weapons grants XP in the corresponding weapon skill
- [ ] Mining ore/stone grants Mining XP
- [ ] Chopping trees grants Woodcutting XP
- [ ] Running grants Running XP (throttled)
- [ ] Swimming grants Swimming XP (throttled)
- [ ] Sneaking grants Sneaking XP (throttled)
- [ ] Jumping grants Jumping XP
- [ ] Blocking attacks grants Blocking XP
- [ ] Level-up notifications displayed to player
- [ ] "Rested" bonus increases XP gain by 50%

---

## Tasks

### Task 2.1 — Create XP calculation utilities

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin

object XpCalculator {
    // All values read from config (Phase 1.5) instead of hardcoded constants
    private val config get() = HytaleSkillsPlugin.instance.config.xp

    fun xpRequiredForLevel(level: Int): Float {
        // XP required to go from level-1 to level
        return config.baseXpPerAction * 100f * (1 + (level - 1) * config.xpScaleFactor)
    }

    fun cumulativeXpForLevel(level: Int): Float {
        // Total XP needed to reach this level from 0
        var total = 0f
        for (i in 1..level) {
            total += xpRequiredForLevel(i)
        }
        return total
    }

    fun calculateXpGain(baseXp: Float, isRested: Boolean): Float {
        val restedBonus = config.restedBonusMultiplier - 1f
        val multiplier = if (isRested) 1f + restedBonus else 1f
        return baseXp * config.globalXpMultiplier * multiplier
    }
}
```

### Task 2.2 — Create `SkillXpService.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

object SkillXpService {

    fun grantXp(
        playerRef: Ref<EntityStore>,
        skillType: SkillType,
        baseXp: Float,
        isRested: Boolean = false
    ): LevelUpResult? {
        val skills = SkillManager.getPlayerSkills(playerRef)
        val skillData = skills.getSkill(skillType)

        if (skillData.level >= SkillData.MAX_LEVEL) {
            return null // Already max level
        }

        val xpGain = XpCalculator.calculateXpGain(baseXp, isRested)
        skillData.totalXp += xpGain

        val oldLevel = skillData.level
        var newLevel = oldLevel

        // Check for level ups
        while (newLevel < SkillData.MAX_LEVEL) {
            val xpForNext = XpCalculator.cumulativeXpForLevel(newLevel + 1)
            if (skillData.totalXp >= xpForNext) {
                newLevel++
            } else {
                break
            }
        }

        skillData.level = newLevel
        SkillManager.savePlayerSkills(playerRef, skills)

        return if (newLevel > oldLevel) {
            LevelUpResult(skillType, oldLevel, newLevel)
        } else {
            null
        }
    }

    fun notifyLevelUp(playerRef: Ref<EntityStore>, result: LevelUpResult) {
        // Send level up message to player
        val message = "§a${result.skillType.displayName} increased to level ${result.newLevel}!"
        // TODO: Use proper Hytale message API
    }
}

data class LevelUpResult(
    val skillType: SkillType,
    val oldLevel: Int,
    val newLevel: Int
)
```

### Task 2.3 — Create `CombatListener.kt`

Listen for damage events to grant weapon skill XP:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillXpService

class CombatListener {

    private val config get() = HytaleSkillsPlugin.instance.config.xp.actionXp

    // TODO: Research actual Hytale damage event API — event type is pseudo-code
    fun onPlayerDealDamage(event: /* DamageEvent */) {
        val attacker = event.attacker
        if (!attacker.isPlayer) return

        val playerRef = attacker.asPlayerRef()
        val weapon = event.weapon // Get equipped weapon

        val skillType = when {
            weapon.isSword() -> SkillType.SWORDS
            weapon.isAxe() -> SkillType.AXES
            weapon.isBow() -> SkillType.BOWS
            weapon.isSpear() -> SkillType.SPEARS
            weapon.isClub() -> SkillType.CLUBS
            weapon == null -> SkillType.UNARMED
            else -> return // Unknown weapon type
        }

        // Grant XP based on damage dealt (multiplier from config)
        val baseXp = event.damage * config.combatDamageMultiplier
        val result = SkillXpService.grantXp(playerRef, skillType, baseXp)
        result?.let { SkillXpService.notifyLevelUp(playerRef, it) }
    }
}
```

### Task 2.4 — Create `HarvestListener.kt`

Listen for block break events to grant gathering skill XP:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin

class HarvestListener {

    private val config get() = HytaleSkillsPlugin.instance.config.xp.actionXp

    // TODO: Research actual Hytale block break event API — event type is pseudo-code
    fun onBlockBreak(event: /* BlockBreakEvent */) {
        val player = event.player
        val block = event.block
        val tool = event.tool

        val (skillType, baseXp) = when {
            block.isOre() || block.isStone() -> SkillType.MINING to config.miningPerBlock
            block.isWood() && tool.isAxe() -> SkillType.WOODCUTTING to config.woodcuttingPerBlock
            else -> return
        }

        val playerRef = player.asPlayerRef()
        val result = SkillXpService.grantXp(playerRef, skillType, baseXp)
        result?.let { SkillXpService.notifyLevelUp(playerRef, it) }
    }
}
```

### Task 2.5 — Create `MovementListener.kt`

Track movement actions with throttling to prevent spam:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MovementListener {

    private val config get() = HytaleSkillsPlugin.instance.config.xp.actionXp

    // Cooldowns to prevent XP spam (milliseconds)
    private val runningCooldowns = ConcurrentHashMap<UUID, Long>()
    private val swimmingCooldowns = ConcurrentHashMap<UUID, Long>()
    private val sneakingCooldowns = ConcurrentHashMap<UUID, Long>()

    companion object {
        const val MOVEMENT_XP_COOLDOWN = 1000L // 1 second between XP grants
    }

    // TODO: Research actual Hytale movement event API — event type is pseudo-code
    fun onPlayerMove(event: /* PlayerMoveEvent */) {
        val player = event.player
        val playerId = player.uuid
        val playerRef = player.asPlayerRef()
        val now = System.currentTimeMillis()

        when {
            player.isRunning() -> {
                if (canGrantXp(runningCooldowns, playerId, now)) {
                    SkillXpService.grantXp(playerRef, SkillType.RUNNING, config.runningPerSecond)
                    runningCooldowns[playerId] = now
                }
            }
            player.isSwimming() -> {
                if (canGrantXp(swimmingCooldowns, playerId, now)) {
                    SkillXpService.grantXp(playerRef, SkillType.SWIMMING, config.swimmingPerSecond)
                    swimmingCooldowns[playerId] = now
                }
            }
            player.isSneaking() -> {
                if (canGrantXp(sneakingCooldowns, playerId, now)) {
                    SkillXpService.grantXp(playerRef, SkillType.SNEAKING, config.sneakingPerSecond)
                    sneakingCooldowns[playerId] = now
                }
            }
        }
    }

    // TODO: Research actual Hytale jump event API — event type is pseudo-code
    fun onPlayerJump(event: /* PlayerJumpEvent */) {
        val playerRef = event.player.asPlayerRef()
        SkillXpService.grantXp(playerRef, SkillType.JUMPING, config.jumpingPerJump)
    }

    private fun canGrantXp(cooldowns: ConcurrentHashMap<UUID, Long>, playerId: UUID, now: Long): Boolean {
        val lastGrant = cooldowns[playerId] ?: 0L
        return now - lastGrant >= MOVEMENT_XP_COOLDOWN
    }
}
```

### Task 2.6 — Create `BlockingListener.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin

class BlockingListener {

    private val config get() = HytaleSkillsPlugin.instance.config.xp.actionXp

    // TODO: Research actual Hytale block/parry event API — event type is pseudo-code
    fun onPlayerBlock(event: /* BlockEvent */) {
        val player = event.player
        val damageBlocked = event.damageBlocked

        val playerRef = player.asPlayerRef()
        val baseXp = damageBlocked * config.blockingDamageMultiplier
        val result = SkillXpService.grantXp(playerRef, SkillType.BLOCKING, baseXp)
        result?.let { SkillXpService.notifyLevelUp(playerRef, it) }
    }
}
```

### Task 2.7 — Register listeners in plugin setup

Listeners must be instantiated once and reused — **not** created per event fire:

```kotlin
override fun setup() {
    super.setup()
    // ... existing code ...

    // Instantiate listeners once
    val combatListener = CombatListener()
    val harvestListener = HarvestListener()
    val movementListener = MovementListener()
    val blockingListener = BlockingListener()

    // TODO: Research actual Hytale/Kytale event registration API — event types are pseudo-code
    events {
        on<DamageEvent> { combatListener.onPlayerDealDamage(it) }
        on<BlockBreakEvent> { harvestListener.onBlockBreak(it) }
        on<PlayerMoveEvent> { movementListener.onPlayerMove(it) }
        on<PlayerJumpEvent> { movementListener.onPlayerJump(it) }
        on<BlockEvent> { blockingListener.onPlayerBlock(it) }
    }
}
```

> **Note:** All event names (`DamageEvent`, `BlockBreakEvent`, `PlayerMoveEvent`, etc.) are pseudo-code. The actual Hytale/Kytale event types need to be discovered from SDK documentation.

---

## XP Gain Summary

| Skill | Trigger | Base XP | Notes |
|-------|---------|---------|-------|
| Weapon skills | Deal damage | damage * 0.1 | Per hit |
| Mining | Break ore/stone | 1.0 | Per block |
| Woodcutting | Chop tree | 1.0 | Per block |
| Running | Sprint | 0.1 | 1s cooldown |
| Swimming | Swim | 0.1 | 1s cooldown |
| Sneaking | Sneak | 0.1 | 1s cooldown |
| Jumping | Jump | 0.5 | Per jump |
| Blocking | Block damage | blocked * 0.05 | Per block |

---

## Research Required

Before implementing this phase, confirm the following APIs against actual Hytale/Kytale SDK documentation:

- [ ] **Damage event** — Event type for when a player deals damage, including attacker/weapon/damage fields
- [ ] **Block break event** — Event type for when a player breaks a block, including block type and tool
- [ ] **Movement events** — How to detect running, swimming, sneaking, jumping states
- [ ] **Event registration** — Kytale `events { on<T> { } }` DSL syntax and lifecycle
- [ ] **Weapon/tool type detection** — How to determine what weapon/tool a player is holding
- [ ] **Player messaging** — API for sending chat messages and notifications to players
- [ ] **Block/material type checks** — How to determine if a block is ore, stone, wood, etc.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| No XP gained | Verify event listeners are registered and firing |
| XP spam | Increase cooldown timers |
| Level up not showing | Check notification code, verify message API |
| Wrong skill getting XP | Debug weapon/tool type detection |
