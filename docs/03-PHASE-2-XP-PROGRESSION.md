# Phase 2 — XP Gain & Leveling

## Goal
Implement XP gain from player actions. Players earn XP in relevant skills by performing actions: attacking with weapons, mining, chopping trees, running, swimming, etc.

## Prerequisites
- Phase 1.5 complete (skill data model, persistence, and config system working)

## Status: **Complete**

## Done Criteria
- [x] Attacking enemies with weapons grants XP in the corresponding weapon skill
- [x] Mining ore/stone grants Mining XP
- [x] Chopping trees grants Woodcutting XP
- [x] Running grants Running XP (distance-based)
- [x] Swimming grants Swimming XP (distance-based)
- [x] Diving grants Diving XP (time-based)
- [x] Sneaking grants Sneaking XP (time-based)
- [x] Jumping grants Jumping XP
- [x] Blocking attacks grants Blocking XP
- [x] Level-up notifications displayed to player
- [x] Rested bonus multiplier configurable (default 1.5x)

---

## Tasks

### Task 2.1 — Create `XpCurve.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.xp

class XpCurve(private val xpConfig: XpConfig) {
    fun xpRequiredForLevel(level: Int): Double =
        xpConfig.baseXpPerAction * 100.0 * (1 + (level - 1) * xpConfig.xpScaleFactor)

    fun cumulativeXpForLevel(level: Int): Double =
        (1..level).sumOf { xpRequiredForLevel(it) }

    fun calculateXpGain(baseXp: Double, isRested: Boolean): Double {
        val multiplier = if (isRested) xpConfig.restedBonusMultiplier else 1.0
        return baseXp * xpConfig.globalXpMultiplier * multiplier
    }

    fun levelProgress(level: Int, totalXP: Double, maxLevel: Int): Double {
        if (level >= maxLevel) return 1.0
        val current = cumulativeXpForLevel(level)
        val next = cumulativeXpForLevel(level + 1)
        return ((totalXP - current) / (next - current)).coerceIn(0.0, 1.0)
    }
}
```

> **Architecture:** `XpCurve` is a class (not an object) that takes `XpConfig` via constructor injection. Instantiated once in `SkillsPlugin.setup()`.

### Task 2.2 — Create `XpService.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.xp

class XpService(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val generalConfig: GeneralConfig,
    private val logger: HytaleLogger
) {
    fun grantXp(playerRef: Ref<EntityStore>, skillType: SkillType, baseXp: Double): LevelUpResult? {
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return null
        val skillData = skills.getSkill(skillType)
        if (skillData.level >= generalConfig.maxLevel) return null

        val xpGain = xpCurve.calculateXpGain(baseXp, isRested = false)
        skillData.totalXP += xpGain

        val levelUp = computeLevelUp(skillData, generalConfig.maxLevel)
        return levelUp
    }

    // Variant that persists via CommandBuffer (used in ECS event handlers)
    fun grantXpAndSave(commandBuffer: CommandBuffer<EntityStore>, playerRef: Ref<EntityStore>,
                       skillType: SkillType, baseXp: Double): LevelUpResult? { ... }

    fun notifyLevelUp(player: Player, result: LevelUpResult) {
        if (generalConfig.showLevelUpNotifications) {
            player.sendMessage(Message.raw("${result.skillType.displayName} increased to level ${result.newLevel}!"))
        }
    }
}

data class LevelUpResult(val skillType: SkillType, val oldLevel: Int, val newLevel: Int)
```

> **Architecture:** `XpService` uses constructor injection. `grantXpAndSave()` variant uses `CommandBuffer` for batched ECS updates in event handlers (required when modifying components during system iteration).

### Task 2.3 — Create `CombatListener.kt`

Uses Kytale's Hexweave damage system (runs after `DamageSystems.ApplyDamage`):

```kotlin
class CombatListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger
) {
    fun onPlayerDealDamage(ctx: HexweaveDamageContext) {
        // Filter: only PHYSICAL/PROJECTILE damage from players
        val playerRef = ctx.source?.playerRef ?: return
        val itemId = ctx.source?.itemInHand?.id ?: ""
        val skillType = weaponSkillResolver.resolve(itemId) ?: SkillType.UNARMED
        val baseXp = ctx.damage * actionXpConfig.combatDamageMultiplier
        xpService.grantXpAndSave(ctx.commandBuffer, playerRef, skillType, baseXp)
    }
}
```

**Weapon resolution**: `WeaponSkillResolver` maps Item ID prefixes to skills:
- `Weapon_Sword`, `Weapon_Longsword` → SWORDS
- `Weapon_Daggers` → DAGGERS
- `Weapon_Axe`, `Weapon_Battleaxe` → AXES
- `Weapon_Shortbow`, `Weapon_Crossbow` → BOWS
- `Weapon_Spear` → SPEARS
- `Weapon_Mace`, `Weapon_Club` → CLUBS
- No match / empty hand → UNARMED

### Task 2.4 — Create `HarvestListener.kt`

Uses `DamageBlockEvent` ECS event (registered via `EntityEventSystem`):

```kotlin
class HarvestListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger
) {
    fun onPlayerDamageBlock(ctx: EntityEventContext<EntityStore, DamageBlockEvent>) {
        val blockId = ctx.event.blockType.id
        val skillType = blockSkillResolver.resolve(blockId) ?: return
        val multiplier = when (skillType) {
            SkillType.MINING -> actionXpConfig.miningPerBlockMultiplier
            SkillType.WOODCUTTING -> actionXpConfig.woodcuttingPerBlockMultiplier
            else -> return
        }
        val baseXp = ctx.event.damage * multiplier
        xpService.grantXpAndSave(ctx.commandBuffer, playerRef, skillType, baseXp)
    }
}
```

**Block resolution**: `BlockSkillResolver` maps block ID prefixes to skills:
- `Rock_*`, `Ore_*` → MINING
- `Wood_*` → WOODCUTTING

### Task 2.5 — Create `MovementListener.kt`

Uses Hexweave tick system for periodic movement XP tracking with cooldowns:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import org.zunkree.hytale.plugins.skillsplugin.xp.XpService
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.resolver.MovementXpPolicy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MovementListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val movementXpPolicy: MovementXpPolicy,
    private val logger: HytaleLogger
) {
    // Cooldowns to prevent XP spam (milliseconds)
    private val cooldowns = ConcurrentHashMap<Pair<UUID, SkillType>, Long>()

    companion object {
        const val MOVEMENT_XP_COOLDOWN = 1000L // 1 second between XP grants
    }

    // Called from Hexweave tick system for each online player
    fun onPlayerTick(ctx: HexweaveTickContext) {
        val playerRef = ctx.playerRef
        val playerId = ctx.playerId
        val now = System.currentTimeMillis()

        // Running XP (distance-based)
        if (ctx.isSprinting && canGrantXp(playerId, SkillType.RUNNING, now)) {
            xpService.grantXpAndSave(ctx.commandBuffer, playerRef,
                SkillType.RUNNING, actionXpConfig.runningPerDistanceMultiplier)
        }

        // Swimming XP (distance-based)
        if (ctx.isSwimming && canGrantXp(playerId, SkillType.SWIMMING, now)) {
            xpService.grantXpAndSave(ctx.commandBuffer, playerRef,
                SkillType.SWIMMING, actionXpConfig.swimmingPerDistanceMultiplier)
        }

        // Diving XP (time-based, submerged in fluid)
        if (ctx.isSubmerged && canGrantXp(playerId, SkillType.DIVING, now)) {
            xpService.grantXpAndSave(ctx.commandBuffer, playerRef,
                SkillType.DIVING, actionXpConfig.divingPerSecondMultiplier)
        }

        // Sneaking XP (time-based)
        if (ctx.isSneaking && canGrantXp(playerId, SkillType.SNEAKING, now)) {
            xpService.grantXpAndSave(ctx.commandBuffer, playerRef,
                SkillType.SNEAKING, actionXpConfig.sneakingPerSecondMultiplier)
        }
    }

    // Jumping XP — triggered by jump event, not tick
    fun onPlayerJump(ctx: HexweaveTickContext) {
        xpService.grantXpAndSave(ctx.commandBuffer, ctx.playerRef,
            SkillType.JUMPING, actionXpConfig.jumpingPerJumpMultiplier)
    }

    private fun canGrantXp(playerId: UUID, skillType: SkillType, now: Long): Boolean {
        val key = playerId to skillType
        val lastGrant = cooldowns[key] ?: 0L
        if (now - lastGrant >= MOVEMENT_XP_COOLDOWN) {
            cooldowns[key] = now
            return true
        }
        return false
    }

    fun clearCooldowns(playerId: UUID) {
        cooldowns.keys.removeAll { it.first == playerId }
    }
}
```

> **Architecture:** `MovementListener` uses the Hexweave tick system (not event-based). Player state (sprinting, swimming, sneaking, submerged) is queried each tick. Cooldowns prevent XP spam. `clearCooldowns()` called on player disconnect.

### Task 2.6 — Create `BlockingListener.kt`

Uses Hexweave damage system to detect blocked damage:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

class BlockingListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val logger: HytaleLogger
) {
    fun onPlayerBlockDamage(ctx: HexweaveDamageContext) {
        // Filter: only damage blocked by a player (target is blocking)
        val playerRef = ctx.target?.playerRef ?: return
        val blockedAmount = ctx.blockedDamage ?: return
        if (blockedAmount <= 0.0) return

        val baseXp = blockedAmount * actionXpConfig.blockingDamageMultiplier
        xpService.grantXpAndSave(ctx.commandBuffer, playerRef, SkillType.BLOCKING, baseXp)
    }
}
```

> **Architecture:** `BlockingListener` hooks into the same Hexweave damage pipeline as `CombatListener`, but inspects the target (defender) side. `ctx.blockedDamage` represents the portion of damage absorbed by blocking.

### Task 2.7 — Register listeners in plugin setup

Listeners are instantiated once in `SkillsPlugin.setup()` with dependencies injected. Event registration uses two patterns:

1. **Hexweave** — for combat/blocking (damage pipeline) and movement (tick system)
2. **EntityEventSystem** — for harvest (DamageBlockEvent is an ECS event)
3. **Event registry** — for standard events (PlayerReadyEvent, PlayerDisconnectEvent)

```kotlin
override fun setup() {
    super.setup()
    // ... existing code (config, repository, xpCurve, xpService) ...

    // Instantiate listeners once with DI
    val combatListener = CombatListener(xpService, config.xp.actionXp, weaponSkillResolver, logger)
    val blockingListener = BlockingListener(xpService, config.xp.actionXp, logger)
    val harvestListener = HarvestListener(xpService, config.xp.actionXp, blockSkillResolver, logger)
    val movementListener = MovementListener(xpService, config.xp.actionXp, movementXpPolicy, logger)

    // Hexweave: combat + blocking (damage pipeline)
    enableHexweave {
        damageSystems {
            after(DamageSystems.ApplyDamage) { ctx ->
                combatListener.onPlayerDealDamage(ctx)
                blockingListener.onPlayerBlockDamage(ctx)
            }
        }
        tickSystems {
            // Movement XP via tick
            playerTick { ctx -> movementListener.onPlayerTick(ctx) }
        }
    }

    // ECS event: harvest (DamageBlockEvent)
    EntityEventSystem<EntityStore, DamageBlockEvent> { ctx ->
        harvestListener.onPlayerDamageBlock(ctx)
    }

    // Standard events: player lifecycle
    getEventRegistry().register(PlayerReadyEvent::class.java) { event ->
        // Initialize component if needed
    }
    getEventRegistry().register(PlayerDisconnectEvent::class.java) { event ->
        movementListener.clearCooldowns(event.playerId)
    }
}
```

> **Note:** Three event registration patterns are used: `enableHexweave {}` for damage/tick systems, `EntityEventSystem` for ECS events, and `getEventRegistry().register()` for standard lifecycle events.

---

## XP Gain Summary

| Skill         | Trigger         | Base XP              | Notes                   |
|---------------|-----------------|----------------------|-------------------------|
| Weapon skills | Deal damage     | damage * 0.1         | Per hit (Hexweave)      |
| Mining        | Damage ore/stone| damage * 1.0         | DamageBlockEvent        |
| Woodcutting   | Damage wood     | damage * 1.0         | DamageBlockEvent        |
| Running       | Sprint          | 0.1                  | 1s cooldown (tick)      |
| Swimming      | Swim            | 0.1                  | 1s cooldown (tick)      |
| Diving        | Submerge        | 0.1                  | 1s cooldown (tick)      |
| Sneaking      | Sneak           | 0.1                  | 1s cooldown (tick)      |
| Jumping       | Jump            | 0.5                  | Per jump                |
| Blocking      | Block damage    | blocked * 0.05       | Per block (Hexweave)    |

---

## Validated APIs

- [x] **Hexweave damage system** — `enableHexweave { damageSystems { after(DamageSystems.ApplyDamage) { ctx -> } } }` for combat and blocking XP
- [x] **Hexweave tick system** — `enableHexweave { tickSystems { playerTick { ctx -> } } }` for movement XP
- [x] **DamageBlockEvent** — ECS event via `EntityEventSystem<EntityStore, DamageBlockEvent>` for gathering XP
- [x] **CommandBuffer** — `ctx.commandBuffer` available in Hexweave and EntityEvent contexts for batched component writes
- [x] **Event registry** — `getEventRegistry().register(EventClass::class.java) { }` for lifecycle events
- [x] **Player messaging** — `player.sendMessage(Message.raw("text"))` for level-up notifications
- [x] **WeaponSkillResolver** — Item ID prefixes (`Weapon_Sword`, `Weapon_Axe`, etc.) map to SkillType via hytaleitemids.com
- [x] **BlockSkillResolver** — Block ID prefixes (`Rock_*`, `Ore_*`, `Wood_*`) map to Mining/Woodcutting

---

## Troubleshooting

| Problem                | Solution                                         |
|------------------------|--------------------------------------------------|
| No XP gained           | Verify event listeners are registered and firing |
| XP spam                | Increase cooldown timers                         |
| Level up not showing   | Check notification code, verify message API      |
| Wrong skill getting XP | Debug weapon/tool type detection                 |
