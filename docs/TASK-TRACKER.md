# Task Tracker

## Current Phase: 1 - Skill Core

### Phase 0 Tasks (Setup) — Complete
- [x] Initialize Gradle project structure
- [x] Create settings.gradle.kts
- [x] Create gradle.properties
- [x] Create build.gradle.kts
- [x] Create manifest.json
- [x] Create HytaleSkillsPlugin.kt
- [x] Create .gitignore
- [x] Build and verify JAR
- [x] Test with local server
- [x] Push to GitHub

### Phase 1 Tasks (Skill Core)
- [ ] Research ECS/component APIs (Ref, EntityStore, putComponent, BuilderCodec)
- [ ] Create SkillType enum
- [ ] Create SkillData class
- [ ] Create PlayerSkillsComponent with codec
- [ ] Create SkillManager
- [ ] Update /skills command to show levels
- [ ] Test persistence across restarts

### Phase 1.5 Tasks (Config)
- [ ] Research Kytale jsonConfig API
- [ ] Create SkillsConfig data classes
- [ ] Create default config.json
- [ ] Register config in plugin setup
- [ ] Verify config loading and defaults

### Phase 2 Tasks (XP Progression)
- [ ] Research event/listener APIs (damage, block break, movement, jump)
- [ ] Create XpCalculator
- [ ] Create SkillXpService
- [ ] Create CombatListener
- [ ] Create HarvestListener
- [ ] Create MovementListener
- [ ] Create BlockingListener
- [ ] Register listeners in plugin setup
- [ ] Level-up notifications
- [ ] Rested bonus implementation

### Phase 3 Tasks (Skill Effects)
- [ ] Research damage/stamina/movement modification hooks
- [ ] Create SkillEffectConfig (config-driven)
- [ ] Create DamageModifier
- [ ] Create StaminaModifier
- [ ] Create MovementModifier
- [ ] Create GatheringModifier
- [ ] Hook modifiers into events
- [ ] Test all effect types

### Phase 4 Tasks (Death Penalty)
- [ ] Research death event and status effect APIs
- [ ] Create DeathPenaltyService (config-driven)
- [ ] Create DeathListener
- [ ] Immunity timer implementation
- [ ] Immunity status display
- [ ] /skills immunity command
- [ ] Test death penalty flow

### Phase 5 Tasks (UI)
- [ ] Research Hytale UI system (format, creation, dynamic content)
- [ ] Design skills_menu layout
- [ ] Create SkillsUI.kt
- [ ] Category grouping
- [ ] Progress bars
- [ ] Effect descriptions
- [ ] Immunity banner
- [ ] Keybind support (if available)

### Phase 6 Tasks (Polish & Release)
- [ ] Research permission system and command argument types
- [ ] Admin commands (set/reset/give/reload) with permission checks
- [ ] Performance optimization (caching, throttling)
- [ ] README.md
- [ ] CurseForge release

---

## Notes

- Each phase starts with an API research task — confirm pseudo-code APIs before implementing
- Code examples in docs represent design intent; actual APIs may differ
- Test persistence thoroughly before moving to Phase 2
- Balance XP rates through playtesting
- Config system (Phase 1.5) must be complete before Phases 2-4 to avoid hardcoded constants
