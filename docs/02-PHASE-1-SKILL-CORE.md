# Phase 1 — Skill Data Model & Persistence

## Goal
Define the skill types, create the data model for storing skill levels and XP, and implement persistent storage using Hytale's ECS component system.

## Prerequisites
- Phase 0 complete (plugin loads and `/skills` command works)

## Done Criteria
- [ ] `SkillType` enum with all 13 skills defined
- [ ] `SkillData` class with level, XP, and totalXP fields
- [ ] `PlayerSkillsComponent` ECS component with `BuilderCodec` for persistence
- [ ] Skills persist across server restarts
- [ ] `/skills` command shows current skill levels

---

## Tasks

### Task 1.1 — Create `SkillType.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

enum class SkillType(
    val displayName: String,
    val category: SkillCategory
) {
    // Weapon skills
    SWORDS("Swords", SkillCategory.WEAPON),
    AXES("Axes", SkillCategory.WEAPON),
    BOWS("Bows", SkillCategory.WEAPON),
    SPEARS("Spears", SkillCategory.WEAPON),
    CLUBS("Clubs", SkillCategory.WEAPON),
    UNARMED("Unarmed", SkillCategory.WEAPON),

    // Utility skills
    BLOCKING("Blocking", SkillCategory.UTILITY),
    MINING("Mining", SkillCategory.GATHERING),
    WOODCUTTING("Woodcutting", SkillCategory.GATHERING),
    RUNNING("Running", SkillCategory.MOVEMENT),
    SWIMMING("Swimming", SkillCategory.MOVEMENT),
    SNEAKING("Sneaking", SkillCategory.MOVEMENT),
    JUMPING("Jumping", SkillCategory.MOVEMENT);
}

enum class SkillCategory(val displayName: String) {
    WEAPON("Weapon"),
    UTILITY("Utility"),
    GATHERING("Gathering"),
    MOVEMENT("Movement")
}
```

### Task 1.2 — Create `SkillData.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

data class SkillData(
    var level: Int = 0,
    var totalXp: Float = 0f
) {
    companion object {
        const val MAX_LEVEL = 100
    }

    fun getLevelProgress(): Float {
        // At max level, progress is always 100%
        if (level >= MAX_LEVEL) return 1.0f

        // Returns 0.0 - 1.0 progress to next level
        // Delegates to XpCalculator (defined in Phase 2) for XP thresholds
        val xpForNext = XpCalculator.cumulativeXpForLevel(level + 1)
        val xpForCurrent = XpCalculator.cumulativeXpForLevel(level)
        val xpIntoLevel = totalXp - xpForCurrent
        val xpNeeded = xpForNext - xpForCurrent
        return (xpIntoLevel / xpNeeded).coerceIn(0f, 1f)
    }
}
```

> **Note:** `getLevelProgress()` forward-references `XpCalculator` (Phase 2). During Phase 1 development, you can stub it or inline a simple calculation. The full `XpCalculator` is introduced in Phase 2, Task 2.1.

### Task 1.3 — Create `PlayerSkillsComponent.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

// TODO: Research actual Hytale ECS API — these imports are pseudo-code
import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.builder.BuilderCodec

data class PlayerSkillsComponent(
    val skills: MutableMap<SkillType, SkillData> = mutableMapOf(),
    // Death immunity timestamp — deliberately persistent so immunity survives server restarts.
    // Design choice: if a player dies and the server restarts within the immunity window,
    // they should not be punished again. The minor downside (immunity persists across
    // intentional restarts) is acceptable.
    var deathImmunityUntil: Long = 0L
) {
    companion object {
        // TODO: Research actual BuilderCodec API — this is pseudo-code
        val CODEC: Codec<PlayerSkillsComponent> = BuilderCodec.of(PlayerSkillsComponent::class.java)
            .with("skills", { it.skills }, { map ->
                // Serialize skill map
            })
            .with("deathImmunityUntil", { it.deathImmunityUntil }, Long::class.java)
            .build { PlayerSkillsComponent() }
    }

    fun getSkill(type: SkillType): SkillData {
        return skills.getOrPut(type) { SkillData() }
    }

    fun setSkill(type: SkillType, data: SkillData) {
        skills[type] = data
    }

    fun getAllSkills(): Map<SkillType, SkillData> = skills.toMap()

    fun getTotalLevels(): Int = skills.values.sumOf { it.level }
}
```

> **Note:** The `BuilderCodec`, `Codec`, and related ECS persistence APIs shown here are pseudo-code. Consult actual Hytale/Kytale SDK documentation for the correct serialization approach, especially for maps of custom types.

> **Schema versioning:** If the component fields change between plugin versions, you'll need a migration strategy. Consider adding a `version` field to the component and handling upgrades in the codec's `build` factory.

### Task 1.4 — Create `SkillManager.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

// TODO: Research actual Hytale ECS API — Ref<EntityStore> is pseudo-code
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

object SkillManager {

    fun getPlayerSkills(playerRef: Ref<EntityStore>): PlayerSkillsComponent? {
        val store = playerRef.store ?: return null  // Guard against invalid refs
        return store.getComponent(PlayerSkillsComponent::class.java)
            ?: PlayerSkillsComponent().also {
                store.putComponent(it)  // putComponent for persistence
            }
    }

    fun savePlayerSkills(playerRef: Ref<EntityStore>, skills: PlayerSkillsComponent) {
        playerRef.store?.putComponent(skills)  // TODO: Research actual putComponent API
    }

    fun getSkillLevel(playerRef: Ref<EntityStore>, skillType: SkillType): Int {
        return getPlayerSkills(playerRef)?.getSkill(skillType)?.level ?: 0
    }

    fun getSkillData(playerRef: Ref<EntityStore>, skillType: SkillType): SkillData {
        return getPlayerSkills(playerRef)?.getSkill(skillType) ?: SkillData()
    }
}
```

### Task 1.5 — Update `/skills` command

Update the command to display actual skill levels:

```kotlin
command("skills", "View your skill levels") {
    executes { ctx ->
        val playerRef = ctx.senderAsPlayerRef()
        val skills = SkillManager.getPlayerSkills(playerRef)

        val message = buildString {
            appendLine("=== Your Skills ===")
            for (category in SkillCategory.values()) {
                appendLine("${category.displayName}:")
                SkillType.values()
                    .filter { it.category == category }
                    .forEach { skillType ->
                        val data = skills.getSkill(skillType)
                        appendLine("  ${skillType.displayName}: Lv.${data.level}")
                    }
            }
        }
        ctx.sendMessage(Message.raw(message))
    }
}
```

### Task 1.6 — Test persistence

1. Start server, run `/skills` — should show all skills at level 0
2. Manually set a skill level (add debug command or modify data)
3. Restart server
4. Run `/skills` — skill levels should persist

---

## Implementation Notes

### ECS Persistence
- Use `putComponent()` instead of `addComponent()` for data that must survive restarts
- Components must have a registered `Codec` for serialization
- The component is attached to the player's `EntityStore`

### Player Reference
- `ctx.senderAsPlayerRef()` returns `Ref<EntityStore>` for the player
- Use this reference to get/set components on the player entity

---

## Research Required

Before implementing this phase, confirm the following APIs against actual Hytale/Kytale SDK documentation:

- [ ] **ECS component registration** — How to register a custom component type with the entity system
- [ ] **`Ref<EntityStore>`** — Correct type for player entity references; may differ from pseudo-code
- [ ] **`putComponent()` vs `addComponent()`** — Verify persistence semantics and API signatures
- [ ] **`BuilderCodec<T>`** — Correct codec API for serializing data classes, maps, and enums
- [ ] **`ctx.senderAsPlayerRef()`** — How to get a player entity reference from a command context
- [ ] **Component lifecycle** — When components are loaded/unloaded, thread safety guarantees

---

## Troubleshooting

| Problem               | Solution                                                           |
|-----------------------|--------------------------------------------------------------------|
| Skills don't persist  | Ensure using `putComponent()` not `addComponent()`                 |
| Codec errors          | Verify `BuilderCodec` setup matches data class fields              |
| Null player reference | Check if command sender is actually a player with `ctx.isPlayer()` |
