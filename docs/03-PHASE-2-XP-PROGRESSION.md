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

> **Architecture:** `XpCurve` is a class (not an object) that takes `XpConfig` via constructor injection. Instantiated once in `PluginApplication.setup()`.

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

Uses `CombatXpDamageSystem` (custom `DamageEventSystem` subclass, runs after `DamageSystems.ApplyDamage`):

```kotlin
class CombatListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger
) {
    fun onPlayerDealDamage(ctx: DamageContext) {
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
    fun onPlayerDamageBlock(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        event: DamageBlockEvent,
    ) {
        val ref = chunk.getReferenceTo(index)
        val blockId = event.blockType.id
        val skillType = blockSkillResolver.resolve(blockId) ?: return
        val multiplier = when (skillType) {
            SkillType.MINING -> actionXpConfig.miningPerBlockMultiplier
            SkillType.WOODCUTTING -> actionXpConfig.woodcuttingPerBlockMultiplier
            else -> return
        }
        val baseXp = event.damage * multiplier
        xpService.grantXpAndSave(commandBuffer, ref, skillType, baseXp)
    }
}
```

**Block resolution**: `BlockSkillResolver` maps block ID prefixes to skills:
- `Rock_*`, `Ore_*` → MINING
- `Wood_*` → WOODCUTTING

### Task 2.5 — Create `MovementListener.kt`

Uses `EntityTickingSystem<EntityStore>` for periodic movement XP tracking with cooldowns:

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

    // Called from MovementTickSystem for each online player
    fun onTick(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        deltaTime: Float,
    ) {
        val ref = chunk.getReferenceTo(index)
        val playerId = ref.uuid
        val now = System.currentTimeMillis()

        // Running XP (distance-based)
        if (isSprinting(ref, store) && canGrantXp(playerId, SkillType.RUNNING, now)) {
            xpService.grantXpAndSave(commandBuffer, ref,
                SkillType.RUNNING, actionXpConfig.runningPerDistanceMultiplier)
        }

        // Swimming XP (distance-based)
        if (isSwimming(ref, store) && canGrantXp(playerId, SkillType.SWIMMING, now)) {
            xpService.grantXpAndSave(commandBuffer, ref,
                SkillType.SWIMMING, actionXpConfig.swimmingPerDistanceMultiplier)
        }

        // Diving XP (time-based, submerged in fluid)
        if (isSubmerged(ref, store) && canGrantXp(playerId, SkillType.DIVING, now)) {
            xpService.grantXpAndSave(commandBuffer, ref,
                SkillType.DIVING, actionXpConfig.divingPerSecondMultiplier)
        }

        // Sneaking XP (time-based)
        if (isSneaking(ref, store) && canGrantXp(playerId, SkillType.SNEAKING, now)) {
            xpService.grantXpAndSave(commandBuffer, ref,
                SkillType.SNEAKING, actionXpConfig.sneakingPerSecondMultiplier)
        }
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

> **Architecture:** `MovementListener` uses the tick system (not event-based). Player state (sprinting, swimming, sneaking, submerged) is queried each tick. Cooldowns prevent XP spam. `clearCooldowns()` called on player disconnect.

### Task 2.6 — Create `BlockingListener.kt`

Uses `CombatXpDamageSystem` to detect blocked damage:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

class BlockingListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val logger: HytaleLogger
) {
    fun onPlayerBlockDamage(ctx: DamageContext) {
        // Filter: only damage blocked by a player (target is blocking)
        val playerRef = ctx.target?.playerRef ?: return
        val blockedAmount = ctx.blockedDamage ?: return
        if (blockedAmount <= 0.0) return

        val baseXp = blockedAmount * actionXpConfig.blockingDamageMultiplier
        xpService.grantXpAndSave(ctx.commandBuffer, playerRef, SkillType.BLOCKING, baseXp)
    }
}
```

> **Architecture:** `BlockingListener` hooks into the same `CombatXpDamageSystem` damage pipeline as `CombatListener`, but inspects the target (defender) side. Both are called from a custom `DamageEventSystem` subclass running `after<ApplyDamage>()`.

### Task 2.7 — Register listeners in plugin setup

Listeners are instantiated once in `PluginApplication.setup()` with dependencies injected. Event registration uses three patterns:

1. **Custom DamageEventSystem subclasses** — for combat/blocking (damage pipeline), registered via `entityStoreRegistry.registerSystem()`
2. **EntityEventSystem / EntityTickingSystem** — for gathering (DamageBlockEvent) and movement (tick system), registered via `entityStoreRegistry.registerSystem()`
3. **Event registry** — for standard events (PlayerReadyEvent, PlayerDisconnectEvent), registered via `eventRegistry.register()`

```kotlin
// In PluginApplication.registerAll()
fun registerAll(config: SkillsConfig, s: ServiceGraph) {
    // Standard events: player lifecycle
    plugin.eventRegistry.register(PlayerReadyEvent::class.java) { event ->
        s.playerLifecycleListener.onPlayerReady(event)
    }
    plugin.eventRegistry.register(PlayerDisconnectEvent::class.java) { event ->
        s.playerLifecycleListener.onPlayerDisconnect(event)
        movementListener.onPlayerDisconnect(event)
        movementEffectApplier.onPlayerDisconnect(event.playerRef.uuid)
    }

    // Damage systems: custom DamageEventSystem subclasses
    plugin.entityStoreRegistry.registerSystem(SkillEffectDamageSystem(s.combatEffectApplier, log))
    plugin.entityStoreRegistry.registerSystem(CombatXpDamageSystem(s.combatListener, s.blockingListener, log))

    // ECS event system: gathering (DamageBlockEvent)
    plugin.entityStoreRegistry.registerSystem(BlockDamageXpSystem(s.harvestListener, s.gatheringEffectApplier, log))

    // ECS tick system: movement
    plugin.entityStoreRegistry.registerSystem(MovementTickSystem(movementListener, s.statEffectApplier, movementEffectApplier, log))

    // Command
    plugin.commandRegistry.registerCommand(SkillsCommand(s.skillRepository, s.xpCurve, config.general, s.commandLogger))
}
```

> **Note:** Three event registration patterns are used: custom `DamageEventSystem` subclasses for the damage pipeline, `EntityEventSystem<S,E>` / `EntityTickingSystem<S>` for ECS event and tick systems, and `eventRegistry.register()` for standard lifecycle events.

---

## XP Gain Summary

| Skill         | Trigger         | Base XP              | Notes                   |
|---------------|-----------------|----------------------|-------------------------|
| Weapon skills | Deal damage     | damage * 0.1         | Per hit (DamageEventSystem) |
| Mining        | Damage ore/stone| damage * 1.0         | DamageBlockEvent        |
| Woodcutting   | Damage wood     | damage * 1.0         | DamageBlockEvent        |
| Running       | Sprint          | 0.1                  | 1s cooldown (tick)      |
| Swimming      | Swim            | 0.1                  | 1s cooldown (tick)      |
| Diving        | Submerge        | 0.1                  | 1s cooldown (tick)      |
| Sneaking      | Sneak           | 0.1                  | 1s cooldown (tick)      |
| Jumping       | Jump            | 0.5                  | Per jump                |
| Blocking      | Block damage    | blocked * 0.05       | Per block (DamageEventSystem) |

---

## Validated APIs

- [x] **DamageEventSystem** — Custom subclasses with `SystemDependency(Order.AFTER, DamageSystems.ApplyDamage::class.java)` for combat and blocking XP
- [x] **EntityTickingSystem<EntityStore>** — Native ECS tick system for movement XP, registered via `entityStoreRegistry.registerSystem()`
- [x] **DamageBlockEvent** — ECS event via `EntityEventSystem<EntityStore, DamageBlockEvent>` for gathering XP
- [x] **CommandBuffer** — `commandBuffer` parameter available in damage systems, EntityEventSystem, and EntityTickingSystem for batched component writes
- [x] **Event registry** — `eventRegistry.register(EventClass::class.java) { }` for lifecycle events
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
