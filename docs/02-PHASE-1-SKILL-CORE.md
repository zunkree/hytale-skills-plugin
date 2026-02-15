# Phase 1 — Skill Data Model & Persistence

## Goal
Define the skill types, create the data model for storing skill levels and XP, and implement persistent storage using Hytale's ECS component system.

## Prerequisites
- Phase 0 complete (plugin loads and `/skills` command works)

## Status: **Complete**

## Done Criteria
- [x] `SkillType` enum with all 15 skills defined (added DAGGERS, DIVING)
- [x] `SkillData` class with level and cumulative totalXP (Double)
- [x] `PlayerSkillsComponent` ECS component (`Component<EntityStore>`) with `BuilderCodec` for persistence
- [x] Skills persist across server restarts via `putComponent()`
- [x] `/skills` command shows current skill levels with progress

---

## Tasks

### Task 1.1 — Create `SkillType.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

enum class SkillType(
    val displayName: String,
    val categories: Set<SkillCategory>
) {
    // Weapon skills
    SWORDS("Swords", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),
    DAGGERS("Daggers", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),
    AXES("Axes", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),
    BOWS("Bows", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),
    SPEARS("Spears", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),
    CLUBS("Clubs", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),
    UNARMED("Unarmed", setOf(SkillCategory.WEAPON, SkillCategory.COMBAT)),

    // Utility skills
    BLOCKING("Blocking", setOf(SkillCategory.UTILITY, SkillCategory.COMBAT)),

    // Gathering skills
    MINING("Mining", setOf(SkillCategory.GATHERING)),
    WOODCUTTING("Woodcutting", setOf(SkillCategory.GATHERING)),

    // Movement skills
    RUNNING("Running", setOf(SkillCategory.MOVEMENT)),
    SWIMMING("Swimming", setOf(SkillCategory.MOVEMENT)),
    DIVING("Diving", setOf(SkillCategory.MOVEMENT)),
    SNEAKING("Sneaking", setOf(SkillCategory.MOVEMENT)),
    JUMPING("Jumping", setOf(SkillCategory.MOVEMENT));
}

enum class SkillCategory(val displayName: String) {
    COMBAT("Combat"),
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
    var totalXP: Double = 0.0
) {
    companion object {
        val CODEC: BuilderCodec<SkillData> = // BuilderCodec with "level" (Int) and "totalXP" (Double) fields
    }
}
```

> **Note:** Level progress calculation is handled by `XpCurve.levelProgress()` (Phase 2), not stored in `SkillData`.

### Task 1.3 — Create `PlayerSkillsComponent.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.skill

import com.hypixel.hytale.server.core.modules.entity.Component
import com.hypixel.hytale.server.core.modules.entity.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class PlayerSkillsComponent : Component<EntityStore> {
    val skills: MutableMap<SkillType, SkillData> = SkillType.entries.associateWith { SkillData() }.toMutableMap()
    // Death immunity timestamp — deliberately persistent so immunity survives server restarts.
    var deathImmunityUntil: Long = 0L

    companion object {
        fun getComponentType(): ComponentType<EntityStore, PlayerSkillsComponent> = componentType()

        // BuilderCodec encodes each skill type's level + totalXP, plus deathImmunityUntil
        // Uses SkillType.name as persistence keys for stability
        val CODEC: BuilderCodec<PlayerSkillsComponent> = // ...
    }

    fun getSkill(type: SkillType): SkillData = skills.getOrPut(type) { SkillData() }
    fun setSkill(type: SkillType, data: SkillData) { skills[type] = data }
    val allSkills: Map<SkillType, SkillData> get() = skills.toMap()
    val totalLevels: Int get() = skills.values.sumOf { it.level }
    override fun clone(): PlayerSkillsComponent = // deep copy
}
```

> **Persistence:** Component registered via `entityStoreRegistry.registerComponent(PlayerSkillsComponent.getComponentType(), CODEC)` in plugin `setup()`. Uses `putComponent()` (persistent) not `addComponent()` (temporary).

### Task 1.4 — Create `SkillRepository.kt`

```kotlin
package org.zunkree.hytale.plugins.skillsplugin.persistence

import com.hypixel.hytale.server.core.modules.entity.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class SkillRepository(private val logger: HytaleLogger) {

    fun getPlayerSkills(playerRef: Ref<EntityStore>): PlayerSkillsComponent? {
        val store = playerRef.store
        return store.getComponent(playerRef, PlayerSkillsComponent.getComponentType())
    }

    fun savePlayerSkills(playerRef: Ref<EntityStore>, skills: PlayerSkillsComponent) {
        val store = playerRef.store
        store.putComponent(playerRef, skills)  // putComponent = persistent
    }

    // Overload using CommandBuffer for batched updates (used in listeners)
    fun savePlayerSkills(commandBuffer: CommandBuffer<EntityStore>, playerRef: Ref<EntityStore>, skills: PlayerSkillsComponent) {
        commandBuffer.putComponent(playerRef, skills)
    }

    fun getSkillLevel(playerRef: Ref<EntityStore>, skillType: SkillType): Int =
        getPlayerSkills(playerRef)?.getSkill(skillType)?.level ?: 0
}
```

> **Architecture:** Uses constructor-injected dependencies instead of a static `object`. Instantiated in `SkillsPlugin.setup()` and passed to services that need it.

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

## Validated APIs

All APIs for this phase have been confirmed:

- [x] **ECS component registration** — `entityStoreRegistry.registerComponent(componentType, codec)` in plugin `setup()`
- [x] **`Ref<EntityStore>`** — Player entity reference; obtained via `PlayerRef.getComponentType()` query or event context
- [x] **`putComponent()` vs `addComponent()`** — `putComponent()` persists across restarts, `addComponent()` is temporary
- [x] **`BuilderCodec<T>`** — `BuilderCodec.builder(Class, Supplier).with("name", Codec.TYPE, getter, setter).build()`
- [x] **`ctx.senderAsPlayerRef()`** — Returns `Ref<EntityStore>` from command context
- [x] **Component lifecycle** — Components loaded with entity; ECS access must be on world thread (use `AbstractPlayerCommand` for commands)

---

## Troubleshooting

| Problem               | Solution                                                           |
|-----------------------|--------------------------------------------------------------------|
| Skills don't persist  | Ensure using `putComponent()` not `addComponent()`                 |
| Codec errors          | Verify `BuilderCodec` setup matches data class fields              |
| Null player reference | Check if command sender is actually a player with `ctx.isPlayer()` |
