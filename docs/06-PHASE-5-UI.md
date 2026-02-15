# Phase 5 — Skills UI

## Goal
Create a visual interface for players to view their skill levels, XP progress, and effects. Accessible via `/skills` command or keybind.

## Prerequisites
- Phase 4 complete (death penalty working)

## Done Criteria
- [ ] `/skills` opens a full-screen skill menu
- [ ] All skills displayed with level and XP progress bar
- [ ] Skills grouped by category (Weapon, Utility, Gathering, Movement)
- [ ] Each skill shows its current effect (e.g., "1.5x damage")
- [ ] Total skill levels displayed
- [ ] Death immunity status shown if active
- [ ] Responsive and performant UI

---

## Tasks

### Task 5.1 — Create `.ui` layout file

Hytale uses **BSON `.ui` files** for UI layout definitions. These are placed in the plugin's asset pack or data directory and loaded server-side.

The UI system uses two page types:
- **`BasicCustomUIPage`** — Static display, data sent once when opened
- **`InteractiveCustomUIPage`** — Supports callbacks via `UIEventBuilder`

For the skills menu, use `InteractiveCustomUIPage` (close button needs callback).

**Layout file:** `src/main/resources/ui/skills_menu.ui`

> **Note:** The exact `.ui` file format (BSON structure, element types, layout properties) needs further research. The layout below represents the desired structure:

**Desired layout elements:**
- Full-screen page (~600x400)
- Header with "Skills" title and total level count
- Conditional immunity banner (shown when death immunity is active)
- Scrollable container with skills grouped by category
- Each skill row: name, current effect text, level number, XP progress bar
- Close button in footer

See the **UI Design Mockup** section below for the visual target.

### Task 5.2 — Create `SkillsUI.kt`

Uses `InteractiveCustomUIPage` with `UICommandBuilder` for data and `UIEventBuilder` for callbacks:

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.ui

import com.hypixel.hytale.server.core.modules.entity.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class SkillsUI(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val effectCalculator: SkillEffectCalculator,
    private val deathPenaltyService: DeathPenaltyService,
    private val generalConfig: GeneralConfig
) {
    // InteractiveCustomUIPage loaded from .ui BSON file
    private lateinit var page: InteractiveCustomUIPage

    fun setup() {
        // Load page from .ui file — exact loading API TBD
        // page = InteractiveCustomUIPage.load("skills_menu")
    }

    fun open(playerRef: Ref<EntityStore>) {
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return

        // Build UI data using UICommandBuilder
        val uiData = UICommandBuilder().apply {
            set("total_levels", skills.totalLevels.toString())

            // Immunity banner
            val hasImmunity = deathPenaltyService.hasImmunity(playerRef)
            set("immunity_visible", hasImmunity)
            if (hasImmunity) {
                val seconds = deathPenaltyService.getRemainingImmunitySeconds(playerRef)
                set("immunity_text", "No Skill Drain: ${formatTime(seconds)}")
            }

            // Populate skills grouped by category
            for (category in SkillCategory.entries) {
                val categorySkills = SkillType.entries.filter { category in it.categories }
                for (skillType in categorySkills) {
                    val skillData = skills.getSkill(skillType)
                    val progress = xpCurve.levelProgress(
                        skillData.level, skillData.totalXP, generalConfig.maxSkillLevel
                    )
                    set("${skillType.name}_level", skillData.level.toString())
                    set("${skillType.name}_progress", progress)
                    set("${skillType.name}_effect", getEffectDescription(skillType, skillData.level))
                }
            }
        }

        // Register close button callback via UIEventBuilder
        val events = UIEventBuilder().apply {
            on("close") { /* Close the UI page for the player */ }
        }

        // Show page to player — exact API TBD
        // page.show(playerRef, uiData, events)
    }

    private fun getEffectDescription(skillType: SkillType, level: Int): String {
        return when {
            SkillCategory.WEAPON in skillType.categories -> {
                val mult = effectCalculator.getDamageMultiplier(skillType, level)
                "%.1fx damage".format(mult)
            }
            skillType == SkillType.BLOCKING -> {
                val stamina = (effectCalculator.getStaminaReduction(skillType, level) * 100).toInt()
                "-$stamina% blocking stamina"
            }
            skillType in listOf(SkillType.MINING, SkillType.WOODCUTTING) -> {
                val mult = effectCalculator.getGatheringSpeedMultiplier(skillType, level)
                "%.1fx speed".format(mult)
            }
            skillType == SkillType.RUNNING -> {
                val speed = (effectCalculator.getSpeedBonus(level) * 100).toInt()
                val stamina = (effectCalculator.getStaminaReduction(skillType, level) * 100).toInt()
                "+$speed% speed, -$stamina% stamina"
            }
            skillType in listOf(SkillType.SWIMMING, SkillType.SNEAKING) -> {
                val stamina = (effectCalculator.getStaminaReduction(skillType, level) * 100).toInt()
                "-$stamina% stamina"
            }
            skillType == SkillType.JUMPING -> {
                val mult = effectCalculator.getJumpHeightMultiplier(level)
                "%.1fx height".format(mult)
            }
            else -> ""
        }
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }
}
```

> **Architecture:** `SkillsUI` uses constructor injection. UI data is built with `UICommandBuilder` (key-value pairs sent to the `.ui` template). Callbacks use `UIEventBuilder`. The `.ui` BSON file defines the visual layout; the Kotlin code provides data and behavior.

### Task 5.3 — Update `/skills` command

Uses `AbstractPlayerCommand` for thread-safe ECS access (required when reading components):

```kotlin
// Main /skills command opens UI
class SkillsCommand(private val skillsUI: SkillsUI) : AbstractPlayerCommand() {
    override fun execute(ctx: PlayerCommandContext) {
        skillsUI.open(ctx.playerRef())
    }
}

// /skills text — fallback text-based display
class SkillsTextCommand(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val generalConfig: GeneralConfig
) : AbstractPlayerCommand() {
    override fun execute(ctx: PlayerCommandContext) {
        val playerRef = ctx.playerRef()
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return

        val message = buildString {
            appendLine("=== Your Skills (Total: ${skills.totalLevels}) ===")
            for (category in SkillCategory.entries) {
                appendLine("${category.displayName}:")
                SkillType.entries
                    .filter { category in it.categories }
                    .forEach { skillType ->
                        val data = skills.getSkill(skillType)
                        val progress = (xpCurve.levelProgress(
                            data.level, data.totalXP, generalConfig.maxSkillLevel
                        ) * 100).toInt()
                        appendLine("  ${skillType.displayName}: Lv.${data.level} ($progress%)")
                    }
            }
        }
        ctx.sendMessage(Message.raw(message))
    }
}

// Registered via AbstractCommandCollection for subcommand routing
class SkillsCommandCollection(
    private val skillsCommand: SkillsCommand,
    private val textCommand: SkillsTextCommand,
    private val immunityCommand: ImmunityCommand
    // ... admin commands added in Phase 6
) : AbstractCommandCollection("skills", "Skill commands") {
    override fun register() {
        default(skillsCommand)          // /skills → opens UI
        sub("text", textCommand)         // /skills text
        sub("immunity", immunityCommand) // /skills immunity
    }
}
```

> **Architecture:** Commands use `AbstractPlayerCommand` (thread-safe ECS access on world thread) and `AbstractCommandCollection` (subcommand routing). This is the validated Hytale command pattern.

### Task 5.4 — Add keybind support (optional)

Keybind registration API is unconfirmed. If available, register in `setup()`:

```kotlin
// TODO: Research keybind registration API
// Possible pattern: registerKeybind("skills_menu", Key.K) { player -> skillsUI.open(player.ref()) }
```

---

## UI Design Mockup

```
┌─────────────────────────────────────────────────────────────┐
│                        Skills                    Total: 247 │
├─────────────────────────────────────────────────────────────┤
│ ▓▓▓▓▓▓▓ No Skill Drain: 8:42 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
├─────────────────────────────────────────────────────────────┤
│ WEAPON SKILLS                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Swords          Lv. 45  ████████████░░░░░░░  1.45x dmg  │ │
│ │ Axes            Lv. 32  ████████░░░░░░░░░░░  1.32x dmg  │ │
│ │ Bows            Lv. 67  █████████████████░░  1.67x dmg  │ │
│ │ Spears          Lv. 12  ███░░░░░░░░░░░░░░░░  1.12x dmg  │ │
│ │ Clubs           Lv. 5   █░░░░░░░░░░░░░░░░░░  1.05x dmg  │ │
│ │ Unarmed         Lv. 3   █░░░░░░░░░░░░░░░░░░  1.03x dmg  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ MOVEMENT SKILLS                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Running         Lv. 38  █████████░░░░░░░░░░  +9% spd    │ │
│ │ Swimming        Lv. 15  ████░░░░░░░░░░░░░░░  -7% stam   │ │
│ │ Sneaking        Lv. 22  █████░░░░░░░░░░░░░░  -16% stam  │ │
│ │ Jumping         Lv. 28  ███████░░░░░░░░░░░░  1.14x ht   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│                        [ Close ]                            │
└─────────────────────────────────────────────────────────────┘
```

---

## Validated APIs

- [x] **UI page types** — `BasicCustomUIPage` (static) and `InteractiveCustomUIPage` (with callbacks) confirmed
- [x] **UI data binding** — `UICommandBuilder` for setting key-value data to send to `.ui` templates
- [x] **UI callbacks** — `UIEventBuilder` for registering button click and interaction handlers
- [x] **UI layout format** — `.ui` BSON files define layout structure (server-side, no client mod needed)
- [x] **HudManager** — Manages persistent HUD elements (used for immunity display in Phase 4)
- [x] **AbstractPlayerCommand** — Thread-safe ECS access for commands that read skill data
- [x] **AbstractCommandCollection** — Subcommand routing for `/skills`, `/skills text`, `/skills immunity`
- [x] **`ctx.senderAsPlayerRef()`** — Returns `Ref<EntityStore>` from command context

### Research Still Needed

- [ ] **`.ui` file format** — Exact BSON structure, element types, layout properties, progress bar widgets
- [ ] **Dynamic lists** — How to render variable-length skill lists in `.ui` templates
- [ ] **Keybind registration** — Whether plugins can register custom keybinds for opening UI

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| UI not opening | Check page creation, verify player is valid |
| Skills not showing | Debug skill population loop |
| Progress bar wrong | Verify getLevelProgress() calculation |
| UI lag | Reduce update frequency, cache skill data |
