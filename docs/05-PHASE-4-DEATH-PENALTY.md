# Phase 4 — Death Penalty

## Goal
Implement skill loss on death, similar to Valheim. When players die, they lose a percentage of their skill levels. A temporary immunity period prevents repeated losses.

## Prerequisites
- Phase 3 complete (skill effects working)

## Done Criteria
- [ ] On death, player loses configurable % of each skill level (default 5%)
- [ ] Death penalty applies to all skills simultaneously
- [ ] 10-minute immunity period after death prevents repeated skill loss
- [ ] Immunity timer visible to player
- [ ] Death penalty can be disabled in config
- [ ] "No Skill Drain" status effect shown during immunity

---

## Tasks

### Task 4.1 — Create `DeathPenaltyService.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.HytaleSkillsPlugin

object DeathPenaltyService {

    // All values read from config (Phase 1.5)
    private val deathConfig get() = HytaleSkillsPlugin.instance.config.deathPenalty
    private val penaltyPercent get() = deathConfig.penaltyPercent
    private val immunityDurationMs get() = deathConfig.immunityDurationSeconds * 1000L
    private val enabled get() = deathConfig.enabled

    /**
     * Apply death penalty to player's skills.
     * Returns true if penalty was applied, false if player was immune.
     */
    fun applyDeathPenalty(playerRef: Ref<EntityStore>): DeathPenaltyResult {
        if (!enabled) {
            return DeathPenaltyResult(applied = false, reason = "Death penalty disabled")
        }

        val skills = SkillManager.getPlayerSkills(playerRef)
        val now = System.currentTimeMillis()

        // Check immunity
        if (now < skills.deathImmunityUntil) {
            val remainingMs = skills.deathImmunityUntil - now
            val remainingSeconds = remainingMs / 1000
            return DeathPenaltyResult(
                applied = false,
                reason = "No Skill Drain active (${remainingSeconds}s remaining)"
            )
        }

        // Apply penalty to each skill
        val penalties = mutableMapOf<SkillType, Int>()
        for ((skillType, skillData) in skills.getAllSkills()) {
            if (skillData.level > 0) {
                val oldLevel = skillData.level
                val newLevel = (oldLevel * (1f - penaltyPercent)).toInt()
                skillData.level = newLevel.coerceAtLeast(0)

                // Adjust totalXp to match new level
                skillData.totalXp = XpCalculator.cumulativeXpForLevel(newLevel)

                val levelsLost = oldLevel - newLevel
                if (levelsLost > 0) {
                    penalties[skillType] = levelsLost
                }
            }
        }

        // Grant immunity
        skills.deathImmunityUntil = now + immunityDurationMs
        SkillManager.savePlayerSkills(playerRef, skills)

        return DeathPenaltyResult(
            applied = true,
            reason = "Skills reduced by ${(penaltyPercent * 100).toInt()}%",
            penalties = penalties,
            immunityUntil = skills.deathImmunityUntil
        )
    }

    /**
     * Check if player has death immunity.
     */
    fun hasImmunity(playerRef: Ref<EntityStore>): Boolean {
        val skills = SkillManager.getPlayerSkills(playerRef)
        return System.currentTimeMillis() < skills.deathImmunityUntil
    }

    /**
     * Get remaining immunity time in seconds.
     */
    fun getRemainingImmunitySeconds(playerRef: Ref<EntityStore>): Long {
        val skills = SkillManager.getPlayerSkills(playerRef)
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

### Task 4.2 — Create `DeathListener.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.listener

import org.zunkree.hytale.plugins.skillsplugin.skill.DeathPenaltyService
import com.hypixel.hytale.server.core.Message

class DeathListener {

    // TODO: Research actual Hytale death event API — event type is pseudo-code
    fun onPlayerDeath(event: /* PlayerDeathEvent */) {
        val player = event.player
        val playerRef = player.asPlayerRef()

        val result = DeathPenaltyService.applyDeathPenalty(playerRef)

        if (result.applied) {
            // Notify player of skill loss
            val message = buildString {
                appendLine("§c=== Death Penalty ===")
                appendLine("§7${result.reason}")
                if (result.penalties.isNotEmpty()) {
                    appendLine("§7Skills lost:")
                    result.penalties.forEach { (skill, levels) ->
                        appendLine("  §c-$levels §7${skill.displayName}")
                    }
                }
                appendLine("§aNo Skill Drain active for 10 minutes")
            }
            player.sendMessage(Message.raw(message))
        } else {
            // Player was immune
            player.sendMessage(Message.raw("§a${result.reason} - No skill loss!"))
        }
    }
}
```

### Task 4.3 — Register death listener

```kotlin
override fun setup() {
    super.setup()
    // ... existing code ...

    val deathListener = DeathListener()  // Instantiate once

    // TODO: Research actual Hytale death event API — event type is pseudo-code
    events {
        on<PlayerDeathEvent> { event ->
            deathListener.onPlayerDeath(event)
        }
    }
}
```

### Task 4.4 — Add immunity status display

Show immunity status in the HUD or as a buff icon:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.ui

object ImmunityStatusDisplay {

    fun updateImmunityDisplay(playerRef: /* PlayerRef */) {
        val hasImmunity = DeathPenaltyService.hasImmunity(playerRef)

        if (hasImmunity) {
            val seconds = DeathPenaltyService.getRemainingImmunitySeconds(playerRef)
            val minutes = seconds / 60
            val secs = seconds % 60
            val timeStr = "${minutes}:${secs.toString().padStart(2, '0')}"

            // Show "No Skill Drain" buff/status
            // Implementation depends on Hytale's buff/status API
            showStatusEffect(playerRef, "No Skill Drain", timeStr)
        } else {
            hideStatusEffect(playerRef, "No Skill Drain")
        }
    }

    private fun showStatusEffect(playerRef: /* PlayerRef */, name: String, duration: String) {
        // TODO: Use Hytale's status effect or HUD API
    }

    private fun hideStatusEffect(playerRef: /* PlayerRef */, name: String) {
        // TODO: Use Hytale's status effect or HUD API
    }
}
```

### Task 4.5 — Add `/skills immunity` subcommand

```kotlin
command("skills", "Skill commands") {
    subcommand("immunity", "Check death immunity status") {
        executes { ctx ->
            val playerRef = ctx.senderAsPlayerRef()
            val hasImmunity = DeathPenaltyService.hasImmunity(playerRef)

            val message = if (hasImmunity) {
                val seconds = DeathPenaltyService.getRemainingImmunitySeconds(playerRef)
                "§aNo Skill Drain active: ${seconds}s remaining"
            } else {
                "§7No immunity active. Death will reduce skills by 5%."
            }
            ctx.sendMessage(Message.raw(message))
        }
    }
}
```

---

## Death Penalty Mechanics

### Penalty Calculation
```
newLevel = floor(currentLevel * (1 - penaltyPercent))

With 5% penalty:
- Level 100 → 95 (lose 5 levels)
- Level 50 → 47 (lose ~3 levels)
- Level 20 → 19 (lose 1 level)
- Level 10 → 9 (lose 1 level)
- Level 5 → 4 (lose 1 level)
- Level 1 → 0 (lose 1 level)
```

### Immunity Period
- Granted immediately after death penalty is applied
- Lasts 10 minutes (configurable)
- Prevents any skill loss during this period
- Visual indicator shows remaining time

### Configuration Options
```json
{
    "deathPenalty": {
        "enabled": true,
        "penaltyPercent": 0.05,
        "immunityDurationSeconds": 600
    }
}
```

---

## Research Required

Before implementing this phase, confirm the following APIs against actual Hytale/Kytale SDK documentation:

- [ ] **Death event** — Event type for when a player dies, including player reference and death cause
- [ ] **Status effect / buff API** — How to show a visual status indicator (immunity timer) on the player's HUD
- [ ] **Respawn event** — Whether there's a post-respawn event for showing death penalty notifications

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Penalty not applying | Check death event is firing, verify enabled flag |
| Immunity not working | Check timestamp comparison, system clock |
| Skills going negative | Add `coerceAtLeast(0)` to level calculation |
| XP mismatch after penalty | Recalculate totalXp from new level |
