# Phase 3 — Skill Effects

## Goal
Apply gameplay bonuses based on skill levels. Higher skill levels provide tangible benefits: increased damage, reduced stamina usage, faster actions, etc.

## Prerequisites
- Phase 2 complete (XP gain and leveling working)

## Done Criteria
- [ ] Weapon skills increase damage dealt (up to 2x at level 100)
- [ ] Blocking skill reduces blocking stamina cost (up to 50% at level 100)
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

import org.zunkree.hytale.plugins.skillsplugin.config.SkillEffectEntry
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

class SkillEffectCalculator(
    private val skillEffectsConfig: Map<SkillType, SkillEffectEntry>,
    private val maxLevel: Int,
) {
    /** Linear interpolation: bonus = min + (max - min) * (level / maxLevel) */
    fun getDamageMultiplier(skillType: SkillType, level: Int): Double {
        val entry = skillEffectsConfig[skillType] ?: return 1.0
        val min = entry.minDamage?.toDouble() ?: 1.0
        val max = entry.maxDamage?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getGatheringSpeedMultiplier(skillType: SkillType, level: Int): Double {
        val entry = skillEffectsConfig[skillType] ?: return 1.0
        val min = entry.minSpeed?.toDouble() ?: 1.0
        val max = entry.maxSpeed?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getSpeedBonus(level: Int): Double {
        val entry = skillEffectsConfig[SkillType.RUNNING] ?: return 0.0
        val min = entry.minSpeedBonus?.toDouble() ?: 0.0
        val max = entry.maxSpeedBonus?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    fun getStaminaReduction(skillType: SkillType, level: Int): Double {
        val entry = skillEffectsConfig[skillType] ?: return 0.0
        val min = entry.minStaminaReduction?.toDouble() ?: 0.0
        val max = entry.maxStaminaReduction?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    fun getJumpHeightMultiplier(level: Int): Double {
        val entry = skillEffectsConfig[SkillType.JUMPING] ?: return 1.0
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

Unified service that applies skill effects via damage and tick systems:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import aster.amo.kytale.extension.componentType
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage

class SkillEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger,
) {
    // Called from SkillEffectDamageSystem (before ApplyDamage)
    fun modifyOutgoingDamage(ctx: DamageContext) {
        val source = ctx.damage.source as? Damage.EntitySource ?: return
        val ref = source.ref
        val player = ref.store.getComponent(ref, componentType<Player>()) ?: return
        val skillType = player.inventory.itemInHand
            ?.let { weaponSkillResolver.resolve(it.item.id) }
            ?: SkillType.UNARMED
        val level = skillRepository.getSkillLevel(ref, skillType)
        if (level <= 0) return
        val multiplier = effectCalculator.getDamageMultiplier(skillType, level)
        ctx.damage.setAmount((ctx.damage.amount * multiplier).toFloat())
    }

    // Called from SkillEffectDamageSystem (before ApplyDamage, target side)
    // Blocking in Hytale is all-or-nothing: DamageModifiers are all 0, so
    // 100% of damage is blocked. The real cost is stamina — each block costs
    // damage / StaminaCost.Value stamina. Guard break occurs when stamina
    // runs out. This skill reduces the stamina cost of blocking.
    fun modifyBlockingStamina(ctx: DamageContext) {
        val ref = ctx.playerRef?.reference ?: return

        if (ctx.damage.getIfPresentMetaObject(Damage.BLOCKED) != true) return

        val level = skillRepository.getSkillLevel(ref, SkillType.BLOCKING)
        if (level <= 0) return

        // Reduce stamina drain during blocking via STAMINA_DRAIN_MULTIPLIER MetaKey
        // At level 100: 50% reduction — player can sustain blocking longer
        val staminaReduction = effectCalculator.getStaminaReduction(SkillType.BLOCKING, level)
        if (staminaReduction > 0.0) {
            val currentStaminaMult = ctx.damage.getIfPresentMetaObject(
                Damage.STAMINA_DRAIN_MULTIPLIER
            ) ?: 1.0f
            ctx.damage.putMetaObject(
                Damage.STAMINA_DRAIN_MULTIPLIER,
                currentStaminaMult * (1.0f - staminaReduction.toFloat()),
            )
        }
    }

    // Called from tick system — modifies player attributes each tick
    // TODO: Research exact TickContext API for speed/jump/stamina modification
    fun applyMovementEffects(ctx: TickContext) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        // TODO: Exact API for modifying sprint speed and jump height per tick is speculative
    }

    // Called from tick system — modifies stamina drain
    // TODO: Research exact TickContext API for stamina drain modification
    fun applyStaminaEffects(ctx: TickContext) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        // TODO: Exact API for modifying per-tick stamina drain is speculative
    }
}
```

> **Architecture:** `SkillEffectApplier` is a single class (not multiple `object` singletons) that applies all skill effects. Uses constructor injection for dependencies. Combat effects hook into custom `DamageEventSystem` subclasses (registered directly via Hytale API); movement/stamina effects hook into Hexweave tick systems.
>
> **Blocking implementation note:** Blocking in Hytale is **all-or-nothing** — `WieldingInteraction.DamageModifiers` are all 0 (Physical: 0, Projectile: 0, Poison: 0), meaning 100% of damage is blocked. The real cost is stamina: `StaminaCost { Value: 7, CostType: "Damage" }` → each block costs `damage / 7` stamina. Guard break occurs when stamina runs out. The Blocking skill only reduces stamina cost via `Damage.STAMINA_DRAIN_MULTIPLIER` MetaKey — there is no "block power" to enhance.
>
> **Research needed:** Verify that `Damage.STAMINA_DRAIN_MULTIPLIER` actually affects `StaminaCost.computeStaminaAmountToConsume()`. The stamina cost is computed separately from the MetaKey system — if they are independent, an alternative approach may be needed.

### Task 3.3 — Create `GatheringEffectApplier.kt`

Modifies gathering speed via DamageBlockEvent:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.effect

import aster.amo.hexweave.dsl.systems.context.EntityEventContext
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class GatheringEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger,
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger,
) {
    fun modifyBlockDamage(ctx: EntityEventContext<EntityStore, DamageBlockEvent>) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        val blockId = ctx.event.blockType.id
        val skillType = blockSkillResolver.resolve(blockId) ?: return
        val level = skillRepository.getSkillLevel(ref, skillType)
        val multiplier = effectCalculator.getGatheringSpeedMultiplier(skillType, level)
        ctx.event.damage *= multiplier
    }
}
```

### Task 3.4 — Register effect systems in plugin setup

```kotlin
override fun setup() {
    super.setup()
    // ... existing code (config, repositories, XP services, resolvers, listeners) ...

    val effectCalculator = SkillEffectCalculator(config.skillEffects, config.general.maxLevel)
    val effectApplier = SkillEffectApplier(skillRepository, effectCalculator, weaponSkillResolver, logger)
    val gatheringEffectApplier = GatheringEffectApplier(skillRepository, effectCalculator, blockSkillResolver, logger)

    // Damage systems: registered directly via Hytale API (not Hexweave)
    // Each is a distinct DamageEventSystem subclass, avoiding DynamicDamageSystem duplicate registration
    entityStoreRegistry.registerSystem(SkillEffectDamageSystem(combatEffectApplier, logger))
    entityStoreRegistry.registerSystem(CombatXpDamageSystem(combatListener, blockingListener, logger))

    // Hexweave: entity event + tick systems (gathering, movement)
    registerHexweaveSystems(
        gatheringEffectApplier,
        movementEffectApplier,
        statEffectApplier,
        harvestListener,
        movementListener,
    )
}
```

> **Note:** Damage systems use custom `DamageEventSystem` subclasses registered directly via `entityStoreRegistry.registerSystem()`. This avoids the Hexweave `DynamicDamageSystem` duplicate class limitation (Hytale's `ComponentRegistry.registerSystem()` rejects duplicate class types). `SkillEffectDamageSystem` runs `before<ApplyDamage>()` to modify values; `CombatXpDamageSystem` runs `after<ApplyDamage>()` to grant XP. By the time `before(ApplyDamage)` runs, the `filterDamageGroup` has already executed — `WieldingDamageReduction` has already set `Damage.BLOCKED = true` and zeroed damage.

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
| Blocking | 0% stamina reduction | 50% stamina reduction |
| Mining | 1.0x speed | 1.5x speed |
| Woodcutting | 1.0x speed | 1.5x speed |
| Running | +0% speed, 0% stamina reduction | +25% speed, 33% stamina reduction |
| Swimming | 0% stamina reduction | 50% stamina reduction |
| Diving | TBD (oxygen capacity) | TBD |
| Sneaking | 0% stamina reduction | 75% stamina reduction |
| Jumping | 1.0x height | 1.5x height |

---

## Validated APIs

- [x] **DamageEventSystem** — Custom subclasses registered via `entityStoreRegistry.registerSystem()` with `SystemDependency(Order.BEFORE/AFTER, DamageSystems.ApplyDamage::class.java)` for damage modification and XP grants
- [x] **Hexweave tick system** — `tickSystem("id") { query { componentType<PlayerRef>() }; onTick { this } }` for per-tick effects
- [x] **DamageBlockEvent** — `entityEventSystem<EntityStore, DamageBlockEvent>("id") { onEvent { this } }` to modify gathering speed
- [x] **CommandBuffer** — Available in damage systems, Hexweave, and EntityEvent contexts for batched ECS writes
- [x] **WeaponSkillResolver** — Reused from Phase 2 for weapon skill type detection in damage modifier
- [x] **`Damage.BLOCKED`** — `MetaKey<Boolean>` set to `true` when damage was blocked by `WieldingDamageReduction`
- [x] **`Damage.STAMINA_DRAIN_MULTIPLIER`** — `MetaKey<Float>` controls stamina drain for the damage event; set by `DamageEffects.addToDamage()` during blocking
- [x] **`Damage.getAmount()` / `setAmount(float)`** — Read/modify damage amount; already reduced by blocking when our handler runs
- [x] **`Damage.getIfPresentMetaObject()` / `putMetaObject()`** — Read/write MetaKey values on the Damage object

### Damage Pipeline (confirmed via decompilation)

The `DamageModule` processes damage in this order:
1. **`gatherDamageGroup`** — Collects initial damage data
2. **`filterDamageGroup`** — Modifies damage before application
   - `WieldingDamageReduction` runs here — applies `WieldingInteraction.damageModifiers`, sets `Damage.BLOCKED`, calls `DamageEffects.addToDamage()`
   - `ArmorDamageReduction` runs here — applies armor resistance
3. **`inspectDamageGroup`** — Post-filter inspection
4. **`DamageSystems.ApplyDamage`** — Actually applies damage to the entity
   - `SkillEffectDamageSystem` (`before<ApplyDamage>()`) — modifies damage/blocking stamina
   - `CombatXpDamageSystem` (`after<ApplyDamage>()`) — grants combat/blocking XP

Both are custom `DamageEventSystem` subclasses registered directly, avoiding Hexweave's `DynamicDamageSystem` duplicate class limitation. Blocking has already occurred when these handlers run.

### Blocking Internals (from HytaleServer.jar decompilation)

| Class | Role |
|-------|------|
| `WieldingInteraction` | Config-driven (BSON asset): `damageModifiers: Int2FloatMap`, `knockbackModifiers: Int2DoubleMap`, `angledWielding`, `staminaCost`, `blockedEffects` |
| `WieldingInteraction.AngledWielding` | Directional blocking — `DamageModifiers` are all 0 (all-or-nothing), 180° frontal cone (`Angle: 0, AngleDistance: 90`) |
| `WieldingInteraction.StaminaCost` | `CostType.DAMAGE`: `computeStaminaAmountToConsume(damage, stats) = damage / value` (e.g., value=7 → 1 stamina blocks 7 damage). `CostType.MAX_HEALTH_PERCENTAGE`: `damage / (value * maxHealth)` |
| `DamageEffects` | Has `staminaDrainMultiplier: float`; `addToDamage(Damage)` writes it to the Damage MetaKey |
| `DamageDataComponent` | ECS component tracking `currentWielding: WieldingInteraction`, `lastCombatAction`, `lastDamageTime` |
| `DamageClass` | Enum: `UNKNOWN`, `LIGHT`, `CHARGED`, `SIGNATURE` — attack types |
| `DamageCalculator` | Computes base damage: `baseDamage`, `sequentialModifierStep`, `randomPercentageModifier` |

### Research Still Needed

- [ ] **Blocking stamina MetaKey** — Verify that `Damage.STAMINA_DRAIN_MULTIPLIER` actually affects `StaminaCost.computeStaminaAmountToConsume()`. The stamina cost formula (`damage / value`) is computed separately — if the MetaKey doesn't feed into it, an alternative approach is needed
- [ ] **Stamina tick system** — Exact API for modifying per-tick stamina drain (running/swimming/sneaking); `ctx.staminaDrainMultiplier` in tick context is speculative
- [ ] **Diving/oxygen** — How to detect submerged state and modify oxygen capacity
- [ ] **Attacker identification** — How to get the attacker's `PlayerRef` and held item from `DamageContext` (ctx.playerRef is the target entity)

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Effects not applying | Verify event hooks are correct |
| Wrong multiplier values | Check config and level calculation |
| Performance issues | Cache skill levels, don't query on every frame |
| Effects too strong/weak | Adjust config values |
