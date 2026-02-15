# Phase 4 — Death Penalty

## Goal
Implement skill loss on death, similar to Valheim. When players die, they lose a percentage of their skill levels. A temporary immunity period prevents repeated losses.

## Prerequisites
- Phase 3 complete (skill effects working)

## Done Criteria
- [ ] On death, player loses configurable % of each skill level (default 10%)
- [ ] Death penalty applies to all skills simultaneously
- [ ] 5-minute immunity period after death prevents repeated skill loss (300s default)
- [ ] Immunity timer visible to player (via HUD)
- [ ] Death penalty can be disabled in config
- [ ] "No Skill Drain" status shown during immunity

---

## Tasks

### Task 4.1 — Create `DeathPenaltyService.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

import com.hypixel.hytale.server.core.modules.entity.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class DeathPenaltyService(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val deathPenaltyConfig: DeathPenaltyConfig,
    private val logger: HytaleLogger
) {
    fun applyDeathPenalty(playerRef: Ref<EntityStore>): DeathPenaltyResult {
        if (!deathPenaltyConfig.enabled) {
            return DeathPenaltyResult(applied = false, reason = "Death penalty disabled")
        }

        val skills = skillRepository.getPlayerSkills(playerRef) ?: return DeathPenaltyResult(
            applied = false, reason = "No skill data found"
        )
        val now = System.currentTimeMillis()

        // Check immunity (stored in persistent component)
        if (now < skills.deathImmunityUntil) {
            val remainingSeconds = (skills.deathImmunityUntil - now) / 1000
            return DeathPenaltyResult(
                applied = false,
                reason = "No Skill Drain active (${remainingSeconds}s remaining)"
            )
        }

        // Apply penalty to each skill
        val penalties = mutableMapOf<SkillType, Int>()
        for ((skillType, skillData) in skills.allSkills) {
            if (skillData.level > 0) {
                val oldLevel = skillData.level
                val newLevel = (oldLevel * (1.0 - deathPenaltyConfig.penaltyPercentage)).toInt()
                    .coerceAtLeast(0)
                skillData.level = newLevel
                skillData.totalXP = xpCurve.cumulativeXpForLevel(newLevel)

                val levelsLost = oldLevel - newLevel
                if (levelsLost > 0) {
                    penalties[skillType] = levelsLost
                }
            }
        }

        // Grant immunity
        val immunityDurationMs = deathPenaltyConfig.immunityDurationSeconds * 1000L
        skills.deathImmunityUntil = now + immunityDurationMs
        skillRepository.savePlayerSkills(playerRef, skills)

        return DeathPenaltyResult(
            applied = true,
            reason = "Skills reduced by ${(deathPenaltyConfig.penaltyPercentage * 100).toInt()}%",
            penalties = penalties,
            immunityUntil = skills.deathImmunityUntil
        )
    }

    fun hasImmunity(playerRef: Ref<EntityStore>): Boolean {
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return false
        return System.currentTimeMillis() < skills.deathImmunityUntil
    }

    fun getRemainingImmunitySeconds(playerRef: Ref<EntityStore>): Long {
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return 0L
        val remaining = skills.deathImmunityUntil - System.currentTimeMillis()
        return (remaining / 1000).coerceAtLeast(0)
    }
}

data class DeathPenaltyResult(
    val applied: Boolean,
    val reason: String,
    val penalties: Map<SkillType, Int> = emptyMap(),
    val immunityUntil: Long = 0L
)
```

> **Architecture:** `DeathPenaltyService` uses constructor injection. Immunity timestamp is stored in `PlayerSkillsComponent.deathImmunityUntil` (persistent via `putComponent()`), so immunity survives server restarts.

### Task 4.2 — Create `DeathListener.kt`

Uses the `DeathEvent` from Hytale's DamageModule (must be declared in `manifest.json` dependencies):

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.modules.damage.DeathEvent

class DeathListener(
    private val deathPenaltyService: DeathPenaltyService,
    private val deathPenaltyConfig: DeathPenaltyConfig,
    private val logger: HytaleLogger
) {
    fun onPlayerDeath(event: DeathEvent) {
        val playerRef = event.entityRef  // Ref<EntityStore> of the dead entity
        // Filter: only apply to players
        // TODO: Confirm how to check if entityRef is a player in DeathEvent

        val result = deathPenaltyService.applyDeathPenalty(playerRef)

        if (result.applied) {
            val immunityMinutes = deathPenaltyConfig.immunityDurationSeconds / 60
            val message = buildString {
                appendLine("=== Death Penalty ===")
                appendLine(result.reason)
                if (result.penalties.isNotEmpty()) {
                    appendLine("Skills lost:")
                    result.penalties.forEach { (skill, levels) ->
                        appendLine("  -$levels ${skill.displayName}")
                    }
                }
                appendLine("No Skill Drain active for $immunityMinutes minutes")
            }
            // TODO: Send message to player after respawn
        } else {
            logger.info { "Death penalty not applied: ${result.reason}" }
        }
    }
}
```

> **Manifest dependency:** `DeathEvent` requires adding `"Hytale:DamageModule": "*"` to the `Dependencies` section of `manifest.json`.

### Task 4.3 — Register death listener

```kotlin
override fun setup() {
    super.setup()
    // ... existing code ...

    val deathPenaltyService = DeathPenaltyService(skillRepository, xpCurve, config.deathPenalty, logger)
    val deathListener = DeathListener(deathPenaltyService, config.deathPenalty, logger)

    // DeathEvent from DamageModule — registered via standard event registry
    getEventRegistry().register(DeathEvent::class.java) { event ->
        deathListener.onPlayerDeath(event)
    }
}
```

### Task 4.4 — Add immunity status display via HUD

Uses Hytale's `HudManager` to show immunity countdown on the player's HUD:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.ui

class ImmunityHudDisplay(
    private val deathPenaltyService: DeathPenaltyService,
    private val deathPenaltyConfig: DeathPenaltyConfig
) {
    // Called from Hexweave tick system to update HUD each tick
    fun updateImmunityDisplay(ctx: HexweaveTickContext) {
        if (!deathPenaltyConfig.showImmunityInHud) return
        val playerRef = ctx.playerRef

        if (deathPenaltyService.hasImmunity(playerRef)) {
            val seconds = deathPenaltyService.getRemainingImmunitySeconds(playerRef)
            val minutes = seconds / 60
            val secs = seconds % 60
            val timeStr = "${minutes}:${secs.toString().padStart(2, '0')}"
            // Show via HudManager — exact API TBD
            // HudManager.showStatus(playerRef, "No Skill Drain: $timeStr")
        }
    }
}
```

> **Note:** `HudManager` is a confirmed Hytale API for managing HUD elements. Exact usage for status text/icons needs further research.

### Task 4.5 — Add `/skills immunity` subcommand

Uses `AbstractPlayerCommand` for thread-safe ECS access:

```kotlin
// Registered as subcommand of /skills via AbstractCommandCollection
class ImmunityCommand(
    private val deathPenaltyService: DeathPenaltyService,
    private val deathPenaltyConfig: DeathPenaltyConfig
) : AbstractPlayerCommand() {
    override fun execute(ctx: PlayerCommandContext) {
        val playerRef = ctx.playerRef()
        val hasImmunity = deathPenaltyService.hasImmunity(playerRef)

        val message = if (hasImmunity) {
            val seconds = deathPenaltyService.getRemainingImmunitySeconds(playerRef)
            "No Skill Drain active: ${seconds}s remaining"
        } else {
            val pct = (deathPenaltyConfig.penaltyPercentage * 100).toInt()
            "No immunity active. Death will reduce skills by $pct%."
        }
        ctx.sendMessage(Message.raw(message))
    }
}
```

---

## Death Penalty Mechanics

### Penalty Calculation
```
newLevel = floor(currentLevel * (1 - penaltyPercentage))

With 10% penalty (default):
- Level 100 → 90 (lose 10 levels)
- Level 50 → 45 (lose 5 levels)
- Level 20 → 18 (lose 2 levels)
- Level 10 → 9 (lose 1 level)
- Level 5 → 4 (lose 1 level)
- Level 1 → 0 (lose 1 level)
```

### Immunity Period
- Granted immediately after death penalty is applied
- Lasts 5 minutes / 300 seconds (configurable via `immunityDurationSeconds`)
- Prevents any skill loss during this period
- Immunity timestamp stored in `PlayerSkillsComponent.deathImmunityUntil` (persistent)
- Visual indicator via `HudManager` shows remaining time

### Configuration Options
```json
{
    "deathPenalty": {
        "enabled": true,
        "penaltyPercentage": 0.1,
        "immunityDurationSeconds": 300,
        "showImmunityInHud": true
    }
}
```

---

## Validated APIs

- [x] **DeathEvent** — `com.hypixel.hytale.server.core.modules.damage.DeathEvent`, registered via `getEventRegistry().register()`
- [x] **DamageModule dependency** — Must add `"Hytale:DamageModule": "*"` to `manifest.json` `Dependencies`
- [x] **HudManager** — Confirmed API for managing HUD elements (status display)
- [x] **AbstractPlayerCommand** — Thread-safe ECS access for `/skills immunity` command
- [x] **Persistent immunity** — `deathImmunityUntil: Long` in `PlayerSkillsComponent` survives restarts via `putComponent()`

### Research Still Needed

- [ ] **DeathEvent fields** — Exact fields available (entityRef, cause, killer, etc.)
- [ ] **Post-respawn messaging** — How to send death penalty notification after player respawns
- [ ] **HudManager usage** — Exact API for showing/hiding status text on player HUD

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Penalty not applying | Check death event is firing, verify enabled flag |
| Immunity not working | Check timestamp comparison, system clock |
| Skills going negative | Add `coerceAtLeast(0)` to level calculation |
| XP mismatch after penalty | Recalculate totalXp from new level |
