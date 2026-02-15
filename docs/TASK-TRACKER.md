# Task Tracker

## Current Phase: 4 - Death Penalty

### Phase 0 Tasks (Setup) — Complete
- [x] Initialize Gradle project structure
- [x] Create settings.gradle.kts
- [x] Create gradle.properties
- [x] Create build.gradle.kts
- [x] Create manifest.json
- [x] Create SkillsPlugin.kt
- [x] Create .gitignore
- [x] Build and verify JAR
- [x] Test with local server
- [x] Push to GitHub

### Phase 1 Tasks (Skill Core) — Complete
- [x] Research ECS/component APIs (Ref, EntityStore, putComponent, BuilderCodec)
- [x] Create SkillType enum (15 skills with Set<SkillCategory>)
- [x] Create SkillData class (level: Int, totalXP: Double)
- [x] Create PlayerSkillsComponent (Component<EntityStore> with BuilderCodec)
- [x] Create SkillRepository (constructor injection, CommandBuffer overload)
- [x] Update /skills command to show levels
- [x] Test persistence across restarts

### Phase 1.5 Tasks (Config) — Complete
- [x] Research Kytale jsonConfig API
- [x] Create SkillsConfig data classes (GeneralConfig, XpConfig, ActionXpConfig, DeathPenaltyConfig, SkillEffectEntry)
- [x] Create default config.json
- [x] Register config in plugin setup via `jsonConfig<SkillsConfig>("config")`
- [x] Verify config loading and defaults

### Phase 2 Tasks (XP Progression) — Complete
- [x] Research event/listener APIs (DamageEventSystem, DamageBlockEvent, tick system)
- [x] Create XpCurve (class with constructor injection)
- [x] Create XpService (grantXp, grantXpAndSave with CommandBuffer)
- [x] Create CombatListener (DamageEventSystem + WeaponSkillResolver)
- [x] Create HarvestListener (DamageBlockEvent + BlockSkillResolver)
- [x] Create MovementListener (tick system with cooldowns)
- [x] Create BlockingListener (DamageEventSystem, target side)
- [x] Register listeners in plugin setup (4 patterns: DamageEventSystem, Hexweave, EntityEventSystem, EventRegistry)
- [x] Level-up notifications via player.sendMessage()
- [x] Rested bonus implementation via XpCurve.calculateXpGain()

### Phase 3 Tasks (Skill Effects) — Complete
- [x] Create SkillEffectCalculator (config-driven, linear interpolation)
- [x] Create CombatEffectApplier (damage, blocking stamina via Damage.STAMINA_DRAIN_MULTIPLIER MetaKey)
- [x] Create MovementEffectApplier (speed, jump force via MovementManager)
- [x] Create StatEffectApplier (stamina, oxygen via EntityStatMap modifiers)
- [x] Create GatheringEffectApplier (mining/woodcutting speed via DamageBlockEvent)
- [x] Register effect systems (before ApplyDamage for effects, after for XP)
- [x] Test all effect types

### Phase 4 Tasks (Death Penalty)
- [ ] Create DeathPenaltyService (constructor injection, persistent immunity)
- [ ] Create DeathListener (DeathEvent from DamageModule)
- [ ] Add DamageModule dependency to manifest.json
- [ ] Create ImmunityHudDisplay (HudManager)
- [ ] Create ImmunityCommand (AbstractPlayerCommand)
- [ ] Test death penalty flow

### Phase 5 Tasks (UI)
- [ ] Create skills_menu.ui layout file (BSON format)
- [ ] Create SkillsUI (InteractiveCustomUIPage + UICommandBuilder + UIEventBuilder)
- [ ] Create SkillsCommand (AbstractPlayerCommand, opens UI)
- [ ] Create SkillsTextCommand (AbstractPlayerCommand, text fallback)
- [ ] Create SkillsCommandCollection (AbstractCommandCollection for subcommand routing)
- [ ] Keybind support (if API available)

### Phase 6 Tasks (Polish & Release)
- [ ] Create AdminCommandCollection (set/reset/give/reload via AbstractCommandCollection)
- [ ] Permission checks for admin commands
- [ ] SkillEffectCache for performance optimization
- [ ] README.md
- [ ] CurseForge release

---

## Validated APIs Summary

| API | Usage | Phase |
|-----|-------|-------|
| `Component<EntityStore>` | Persistent player skill data | 1 |
| `putComponent()` / `getComponent()` | ECS persistence | 1 |
| `BuilderCodec<T>` | BSON serialization | 1 |
| `Ref<EntityStore>` | Player entity reference | 1 |
| `CommandBuffer<EntityStore>` | Batched ECS writes in event handlers | 1, 2 |
| `jsonConfig<T>()` | Plugin configuration | 1.5 |
| `DamageEventSystem` subclasses | Combat damage systems (before/after ApplyDamage) | 2, 3 |
| `enableHexweave {}` | Entity event and tick systems | 2, 3 |
| `Damage.BLOCKED` / `STAMINA_DRAIN_MULTIPLIER` | Blocking detection + stamina MetaKeys | 3 |
| `Damage.getAmount()` / `setAmount()` | Read/modify damage amount | 3 |
| `DamageBlockEvent` / `EntityEventSystem` | Gathering events | 2, 3 |
| `getEventRegistry().register()` | Standard event registration | 2, 4 |
| `DeathEvent` (DamageModule) | Death penalty trigger | 4 |
| `BasicCustomUIPage` / `InteractiveCustomUIPage` | Skills UI | 5 |
| `UICommandBuilder` / `UIEventBuilder` | UI data and callbacks | 5 |
| `HudManager` | Immunity display | 4, 5 |
| `AbstractPlayerCommand` | Thread-safe commands | 4, 5, 6 |
| `AbstractCommandCollection` | Subcommand routing | 5, 6 |
| `ArgTypes` | Command argument types | 6 |

## Notes

- All services use constructor injection (not static objects)
- All numeric values are `Double` (not Float)
- SkillType has 15 entries with `categories: Set<SkillCategory>`
- Death penalty: 10% default, 300s immunity, persistent via ECS
- Four event registration patterns: custom DamageEventSystem subclasses, Hexweave (entity events + ticks), EntityEventSystem, EventRegistry
- CI runs quality checks only (ktlint + detekt) — no compilation (HytaleServer.jar unavailable in CI)
- Damage pipeline order: gatherDamageGroup → filterDamageGroup (blocking/armor) → inspectDamageGroup → ApplyDamage
- Blocking is all-or-nothing (DamageModifiers = 0); stamina is the real cost (`damage / StaminaCost.Value`); plugin reduces blocking stamina via `Damage.STAMINA_DRAIN_MULTIPLIER` MetaKey
- `Damage` MetaKeys: `BLOCKED` (Boolean), `STAMINA_DRAIN_MULTIPLIER` (Float), `HIT_LOCATION`, `HIT_ANGLE`, `KNOCKBACK_COMPONENT`
- `DamageClass` enum: UNKNOWN, LIGHT, CHARGED, SIGNATURE
