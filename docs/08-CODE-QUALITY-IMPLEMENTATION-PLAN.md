# Code Quality Implementation Plan

## Goal
Transform the plugin into top-tier production-quality Kotlin code with a strong focus on:

- Readability
- Maintainability
- Extendability
- Idiomatic Kotlin practices
- Clear architecture boundaries
- Reliable quality gates

This plan is structured for cumulative implementation. You can execute end-to-end, then split changes afterward as needed.

## Working Style (ADHD-Friendly)

### Core Rules
1. Work in focused blocks of `60-90 minutes`.
2. Keep only one active objective at a time.
3. End each block with one visible artifact:
   - A passing test
   - A completed refactor of one class/module
   - A green quality command (`test`, `detekt`, `ktlintCheck`)
4. Track progress in a single checkbox file (`docs/REFactor-CHECKLIST.md`).
5. Stop context-switching by parking ideas in a short "Later" list.

### Done Criteria Per Block
1. Code compiles.
2. Tests for touched logic pass.
3. Formatting and static checks pass for touched files.
4. Notes are updated in checklist.

## Phase 0: Foundation and Safety Net

### Objective
Set up non-negotiable quality rails before deep refactoring.

### Tasks
1. Add quality tooling in `build.gradle.kts`:
   - `ktlint`
   - `detekt`
   - `kover`
   - `JUnit5` and `MockK`
2. Create test folders:
   - `src/test/kotlin`
   - `src/test/resources`
3. Add static analysis config:
   - `detekt.yml`
   - `.editorconfig` Kotlin rules alignment
4. Add CI workflow to run:
   - `clean build`
   - `test`
   - `detekt`
   - `ktlintCheck`
   - `koverVerify`

### Acceptance Criteria
1. Repository builds cleanly with quality checks enabled.
2. Test task runs successfully in CI and local.
3. No business logic changes in this phase.

## Phase 1: Package and Naming Modernization

### Objective
Make the codebase self-explanatory via consistent domain-first naming and structure.

### Tasks
1. Reorganize package structure:
   - `.../skill/domain`
   - `.../skill/application`
   - `.../skill/infrastructure`
   - `.../plugin/listener`
   - `.../plugin/command`
2. Rename pluralized or vague symbols to idiomatic singular domain names:
   - `SkillsType` -> `SkillType`
   - `SkillsData` -> `SkillData`
   - `SkillsComponent` -> `PlayerSkillsComponent`
   - `SkillsXpCalculator` -> `XpCurve`
   - `SkillsXpService` -> `XpGrantService`
3. Keep temporary typealiases only if needed during migration.
4. Update imports and remove dead names after migration is stable.

### Acceptance Criteria
1. Build passes after all renames.
2. Names reflect clear domain language.
3. Package layout matches module responsibilities.

## Phase 2: Remove Global Coupling and Implicit State

### Objective
Eliminate hard coupling to `SkillsPlugin.instance` from business logic.

### Tasks
1. Convert `object` service singletons into classes with constructor dependencies.
2. Keep plugin entrypoint as composition root only.
3. Instantiate services once in `SkillsPlugin.setup()`.
4. Inject dependencies into listeners/handlers via constructor.
5. Restrict `SkillsPlugin.instance` usage to lifecycle-only paths if framework requires it.

### Acceptance Criteria
1. Business logic does not reach global plugin singleton.
2. Core services are testable without plugin runtime boot.
3. Listener code uses injected collaborators only.

## Phase 3: Persistence and Data Contract Hardening

### Objective
Make persisted data stable, future-proof, and backward-compatible.

### Tasks
1. Use stable persistence keys for skills:
   - Prefer `SkillType.name` or explicit immutable key.
   - Do not use display names for serialization keys.
2. Serialize all persistent fields, including `deathImmunityUntil`.
3. Add backward-read fallback for old keys.
4. Auto-rewrite old format to new format on next save.
5. Add codec roundtrip tests and migration tests.

### Acceptance Criteria
1. Old data loads correctly.
2. New saves use stable keys only.
3. `deathImmunityUntil` survives restart/load cycles.

## Phase 4: Domain Invariants and Config Validation

### Objective
Centralize rules and prevent invalid runtime states.

### Tasks
1. Make one source of truth for max level and XP bounds.
2. Remove duplicated constants (for example hardcoded max level inside model class).
3. Add startup config validation:
   - `maxLevel > 0`
   - multipliers `>= 0`
   - death penalty in `[0, 1]`
   - immunity duration `>= 0`
4. Fail fast with precise error messages when config is invalid.
5. Add tests for validation failures and valid boundaries.

### Acceptance Criteria
1. Invalid config fails startup deterministically.
2. Domain logic cannot create out-of-range levels or XP states.
3. Rule ownership is clear and centralized.

## Phase 5: Separate Event Adapters from Domain Logic

### Objective
Reduce complexity in listeners and isolate business rules.

### Tasks
1. Extract resolver/policy classes:
   - `WeaponSkillResolver`
   - `BlockSkillResolver`
   - `MovementXpPolicy`
2. Introduce application use-cases:
   - `GrantSkillXpUseCase`
   - `HandlePlayerReadyUseCase`
   - `HandlePlayerDisconnectUseCase`
3. Refactor listeners into thin adapters:
   - Parse event
   - Map event data
   - Call use-case
4. Remove XP formulas and persistence operations from listener bodies.

### Acceptance Criteria
1. Listener classes are short and straightforward.
2. Business behavior is concentrated in application/domain classes.
3. Event handling changes do not require domain rewrites.

## Phase 6: Performance and Hot-Path Hygiene

### Objective
Cut unnecessary overhead in high-frequency code paths.

### Tasks
1. Replace repeated cumulative XP loops with:
   - O(1) closed-form formula, or
   - precomputed XP table at startup.
2. Reduce info-level logging in per-tick/per-hit paths.
3. Keep hot-path details in debug level only.
4. Cache repeated lookups where safe.
5. Add simple measurement notes before/after optimization.

### Acceptance Criteria
1. No noisy info logs inside high-frequency loops.
2. XP computation is efficient and deterministic.
3. Behavior remains unchanged unless intentionally documented.

## Phase 7: Testing Expansion

### Objective
Lock core behavior with fast, deterministic tests.

### Tasks
1. Add unit tests for:
   - XP curve math
   - level-up boundary behavior
   - resolver mappings
   - death penalty and immunity rules
2. Add characterization tests for existing command output and XP events.
3. Add integration-style tests for persistence adapter save/load.
4. Enforce baseline coverage threshold with Kover.

### Acceptance Criteria
1. Critical domain and application paths are covered.
2. Regressions are caught by automated tests.
3. Coverage threshold is enforced in CI.

## Phase 8: Documentation and Team Operability

### Objective
Make the project easy to maintain and contribute to long-term.

### Tasks
1. Expand `README.md` with:
   - architecture overview
   - module map
   - local dev commands
   - test and lint commands
   - config examples
2. Add `CONTRIBUTING.md` with coding and review standards.
3. Add ADRs in `docs/adr/` for major decisions:
   - serialization key strategy
   - dependency wiring strategy
   - XP curve approach
4. Add "how to add a new skill" guide.

### Acceptance Criteria
1. New contributors can run and modify project from docs alone.
2. Architectural decisions are explicit and discoverable.
3. Contribution quality is standardized.

## Command Checklist

Run these repeatedly during implementation:

1. `./gradlew clean build`
2. `./gradlew test`
3. `./gradlew detekt`
4. `./gradlew ktlintCheck`
5. `./gradlew koverHtmlReport`

## Execution Checklist Template

Create `docs/REFactor-CHECKLIST.md` and track cumulative progress:

```md
# Refactor Checklist

## Phase 0
- [ ] Tooling added
- [ ] CI checks running
- [ ] Test scaffold created

## Phase 1
- [ ] Package restructuring complete
- [ ] Core renames complete

## Phase 2
- [ ] Constructor injection in place
- [ ] Business logic decoupled from plugin singleton

## Phase 3
- [ ] Stable persistence keys
- [ ] deathImmunityUntil persisted
- [ ] Migration tests added

## Phase 4
- [ ] Config validation implemented
- [ ] Domain invariants centralized

## Phase 5
- [ ] Listener logic thinned
- [ ] Use-cases extracted

## Phase 6
- [ ] XP computation optimized
- [ ] Hot-path logs cleaned

## Phase 7
- [ ] Core tests added
- [ ] Coverage gate enabled

## Phase 8
- [ ] README expanded
- [ ] CONTRIBUTING added
- [ ] ADRs added
```

## Final Definition of Done

1. Architecture is domain-first and clear.
2. Core logic is decoupled, testable, and deterministic.
3. Persistence contracts are stable and migration-safe.
4. Static analysis, formatting, tests, and coverage run automatically.
5. Documentation supports long-term maintainability and extension.
