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

### Task 5.1 — Design UI layout

> **Important:** The XML syntax and UI API shown below are **illustrative pseudo-code** representing the desired layout and behavior. The actual Hytale UI system (file format, layout engine, templating, and Kotlin API) needs to be discovered from SDK documentation before implementation.

**Desired layout elements:**
- Full-screen page (~600x400)
- Header with "Skills" title and total level count
- Conditional immunity banner (shown when death immunity is active)
- Scrollable container with skills grouped by category
- Each skill row: name, current effect text, level number, XP progress bar
- Close button in footer

**Illustrative layout (pseudo-XML):**

```xml
<!-- PSEUDO-CODE — actual Hytale UI format TBD -->
<page id="skills_menu" width="600" height="400">
    <panel id="header" height="40" layout="horizontal" align="center">
        <text id="title" text="Skills" font-size="24" color="#ffffff"/>
        <spacer flex="1"/>
        <text id="total_levels" text="Total: 0" font-size="16" color="#aaaaaa"/>
    </panel>

    <panel id="immunity_banner" height="30" visible="false" background="#2a5a2a">
        <text id="immunity_text" text="No Skill Drain: 0:00" color="#66ff66" align="center"/>
    </panel>

    <scroll-panel id="skills_container" flex="1">
        <!-- Categories populated dynamically from SkillCategory enum -->
    </scroll-panel>

    <panel id="footer" height="30">
        <button id="close_btn" text="Close" width="100" on-click="close"/>
    </panel>
</page>

<!-- Each skill rendered as a row with: name, effect text, level, XP bar -->
```

See the **UI Design Mockup** section below for the visual target.

### Task 5.2 — Create `SkillsUI.kt`

> **Note:** The UI construction methods below (`createPage`, `createFromTemplate`, `showPage`, `setText`, `setProgress`, etc.) are **pseudo-code** representing the desired behavior. Actual implementation depends entirely on the Hytale UI API.

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.ui

import org.zunkree.hytale.plugins.skillsplugin.skill.*
import org.zunkree.hytale.plugins.skillsplugin.effect.SkillEffects
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class SkillsUI(private val playerRef: Ref<EntityStore>) {

    fun open() {
        val skills = SkillManager.getPlayerSkills(playerRef)

        // Create and configure UI page
        val page = createPage("skills_menu")

        // Set total levels
        page.setText("total_levels", "Total: ${skills.getTotalLevels()}")

        // Show immunity banner if active
        if (DeathPenaltyService.hasImmunity(playerRef)) {
            val seconds = DeathPenaltyService.getRemainingImmunitySeconds(playerRef)
            val minutes = seconds / 60
            val secs = seconds % 60
            page.setVisible("immunity_banner", true)
            page.setText("immunity_text", "No Skill Drain: ${minutes}:${secs.toString().padStart(2, '0')}")
        }

        // Populate skills by category
        populateCategory(page, "weapon_skills", SkillCategory.WEAPON, skills)
        populateCategory(page, "utility_skills", SkillCategory.UTILITY, skills)
        populateCategory(page, "gathering_skills", SkillCategory.GATHERING, skills)
        populateCategory(page, "movement_skills", SkillCategory.MOVEMENT, skills)

        // Show to player
        showPage(playerRef, page)
    }

    private fun populateCategory(
        page: /* UIPage */,
        containerId: String,
        category: SkillCategory,
        playerSkills: PlayerSkillsComponent
    ) {
        val categorySkills = SkillType.values().filter { it.category == category }

        for (skillType in categorySkills) {
            val skillData = playerSkills.getSkill(skillType)
            val effectText = getEffectDescription(skillType, skillData.level)

            // Create skill row from template
            val row = createFromTemplate("skill_row")
            row.setText("skill_name", skillType.displayName)
            row.setText("skill_level", "Lv. ${skillData.level}")
            row.setText("skill_effect", effectText)
            row.setProgress("skill_xp", skillData.getLevelProgress())

            page.addChild(containerId, row)
        }
    }

    private fun getEffectDescription(skillType: SkillType, level: Int): String {
        val config = SkillEffects.getConfig(skillType)

        return when (skillType.category) {
            SkillCategory.WEAPON -> {
                val mult = config.getMultiplierAtLevel(level)
                "%.1fx damage".format(mult)
            }
            SkillCategory.UTILITY -> {
                when (skillType) {
                    SkillType.BLOCKING -> {
                        val mult = config.getMultiplierAtLevel(level)
                        "%.1fx block power".format(mult)
                    }
                    else -> ""
                }
            }
            SkillCategory.GATHERING -> {
                val mult = config.getMultiplierAtLevel(level)
                "%.1fx speed".format(mult)
            }
            SkillCategory.MOVEMENT -> {
                when (skillType) {
                    SkillType.RUNNING -> {
                        val speed = (config.getSpeedBonusAtLevel(level) * 100).toInt()
                        val stamina = (config.getStaminaReductionAtLevel(level) * 100).toInt()
                        "+$speed% speed, -$stamina% stamina"
                    }
                    SkillType.SWIMMING, SkillType.SNEAKING -> {
                        val stamina = (config.getStaminaReductionAtLevel(level) * 100).toInt()
                        "-$stamina% stamina"
                    }
                    SkillType.JUMPING -> {
                        val mult = config.getMultiplierAtLevel(level)
                        "%.1fx height".format(mult)
                    }
                    else -> ""
                }
            }
        }
    }

    // Placeholder methods - implement with Hytale UI API
    private fun createPage(templateId: String): Any = TODO()
    private fun createFromTemplate(templateId: String): Any = TODO()
    private fun showPage(playerRef: Ref<EntityStore>, page: Any) = TODO()
}
```

### Task 5.3 — Update `/skills` command

```kotlin
command("skills", "View your skill levels") {
    executesSync { ctx ->
        val playerRef = ctx.senderAsPlayerRef()
        SkillsUI(playerRef).open()
    }

    subcommand("text", "Show skills as text") {
        executes { ctx ->
            // Original text-based output
            val playerRef = ctx.senderAsPlayerRef()
            val skills = SkillManager.getPlayerSkills(playerRef)

            val message = buildString {
                appendLine("=== Your Skills (Total: ${skills.getTotalLevels()}) ===")
                for (category in SkillCategory.values()) {
                    appendLine("${category.displayName}:")
                    SkillType.values()
                        .filter { it.category == category }
                        .forEach { skillType ->
                            val data = skills.getSkill(skillType)
                            val progress = (data.getLevelProgress() * 100).toInt()
                            appendLine("  ${skillType.displayName}: Lv.${data.level} ($progress%)")
                        }
                }
            }
            ctx.sendMessage(Message.raw(message))
        }
    }

    subcommand("immunity", "Check death immunity") {
        // ... existing implementation
    }
}
```

### Task 5.4 — Add keybind support (optional)

If Hytale supports custom keybinds:

```kotlin
// PSEUDO-CODE — keybind registration API is unconfirmed
// In setup()
registerKeybind("skills_menu", Key.K) { player ->
    val playerRef = player.asPlayerRef()
    SkillsUI(playerRef).open()
}
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

## Research Required

This phase is the most API-dependent. Before implementing, confirm:

- [ ] **UI file format** — What format Hytale uses for UI definitions (XML, JSON, Kotlin DSL, or something else)
- [ ] **UI page creation** — How to create, populate, and show a full-screen UI to a player from server-side code
- [ ] **Dynamic content** — How to populate UI elements dynamically (loops, templates, data binding)
- [ ] **Progress bars** — Whether Hytale has built-in progress bar widgets or if they need to be composed
- [ ] **Keybind registration** — Whether plugins can register custom keybinds
- [ ] **UI update/refresh** — How to update UI content without closing and reopening (for live XP progress)
- [ ] **Server-side UI rendering** — Whether UI is defined server-side and sent to client, or requires asset pack

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| UI not opening | Check page creation, verify player is valid |
| Skills not showing | Debug skill population loop |
| Progress bar wrong | Verify getLevelProgress() calculation |
| UI lag | Reduce update frequency, cache skill data |
