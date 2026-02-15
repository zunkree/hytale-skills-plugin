# Hytale Skills Plugin: Native Hypixel Plugin Design (No Kytale, No Hexweave)

## TL;DR

This document defines the target architecture for rewriting `skillsplugin` to use **only Hypixel's native server API**.

- Plugin base class: `com.hypixel.hytale.server.core.plugin.JavaPlugin`
- Lifecycle model: `setup()` -> `start()` -> `shutdown()`
- Persistence: ECS `putComponent()` + `BuilderCodec`
- Combat hooks: native `DamageEventSystem` with dependency ordering around `DamageSystems.ApplyDamage`
- Commands: native `AbstractPlayerCommand` + `CommandRegistry.registerCommand(...)`
- Event listeners: native `EventRegistry.register(...)`
- No framework wrappers: remove Kytale DSL and Hexweave DSL from runtime architecture

This is a design spec, not an implementation patch.

---

## Why We Are Changing

### Current pain points

- Runtime and wiring rely on helper DSLs that hide core lifecycle mechanics.
- Plugin behavior is harder to validate against server internals because registration paths are abstracted.
- Migration risk increases when wrapper APIs drift from underlying Hypixel APIs.

### Desired outcome

- Every major behavior maps directly to a Hypixel API type and method.
- Architecture is explicit, testable, and easy to reason about from decompiled signatures.
- Plugin ownership remains with this project, not external framework abstractions.

---

## Scope and Non-Goals

## In scope

- Native plugin bootstrapping and composition root
- Native config loading/saving
- Native command and event registration
- Native ECS component registration and persistence
- Native damage pipeline integration
- Migration plan from current codebase

## Out of scope

- Rebalancing skill values
- New game design mechanics
- UI redesign details beyond architecture boundaries

---

## Verified API Reference (Patchline: `release`)

The following signatures were verified from local `HytaleServer.jar` decompilation.

### Plugin lifecycle

- `JavaPlugin extends PluginBase`
- `PluginBase` exposes:
  - `protected void setup()`
  - `protected void start()`
  - `protected void shutdown()`
  - `getCommandRegistry()`
  - `getEventRegistry()`
  - `getEntityStoreRegistry()`
  - `withConfig(String, BuilderCodec<T>)`

### Configuration

- `com.hypixel.hytale.server.core.util.Config<T>` exposes:
  - `CompletableFuture<T> load()`
  - `T get()`
  - `CompletableFuture<Void> save()`

### Events

- `EventRegistry.register(Class<? super EventType>, Consumer<EventType>)`
- Priority overloads and keyed/global variants exist.

### Commands

- `CommandRegistry.registerCommand(AbstractCommand)`
- `AbstractPlayerCommand` execute signature:
  - `execute(CommandContext, Store<EntityStore>, Ref<EntityStore>, PlayerRef, World)`

### ECS components/systems

- `ComponentRegistryProxy.registerComponent(Class, String, BuilderCodec<T>)`
- `ComponentRegistryProxy.registerSystem(ISystem<ECS_TYPE>)`
- `Store.putComponent(...)` (persistent)
- `Store.addComponent(...)` (non-persistent add path)
- `Store.getComponent(...)`

### Damage pipeline hooks

- `DamageEventSystem extends EntityEventSystem<EntityStore, Damage>`
- `DamageSystems.ApplyDamage` exists for dependency ordering.
- `Damage` supports:
  - `getAmount()` / `setAmount(float)`
  - meta keys including `BLOCKED` and `STAMINA_DRAIN_MULTIPLIER`

---

## Architectural Principles

## P1: One composition root

All wiring must happen in one place during `setup()`.

- Instantiate services
- Register components
- Register systems
- Register listeners
- Register commands

No hidden global singletons for runtime dependencies.

## P2: Lifecycle state is explicit

State that survives setup must be held in a dedicated runtime object, not spread across plugin fields.

## P3: Pure domain logic is framework-free

Skill math, level curves, and mapping logic should not import server APIs.

## P4: ECS writes happen on the store/world thread

Command and event entrypoints must respect thread boundaries.

## P5: Persistence is intentional

Use `putComponent()` for saved skill state. Never rely on `addComponent()` for persistent player progression.

---

## Proposed Package Layout (Native Design)

```text
src/main/kotlin/org/zunkree/hytale/plugins/skillsplugin/
├── SkillsPlugin.kt                      # JavaPlugin entrypoint only
├── bootstrap/
│   ├── PluginApplication.kt             # setup/start/shutdown orchestration
│   ├── RuntimeState.kt                  # active runtime refs for shutdown and lifecycle
│   ├── ServiceFactory.kt                # service instantiation
│   ├── RegistryBootstrap.kt             # register components/systems/events/commands
│   └── ShutdownCoordinator.kt           # flush and cleanup behavior
├── config/
│   ├── SkillsConfig.kt
│   ├── SkillsConfigCodec.kt             # BuilderCodec wiring for config
│   └── SkillsConfigValidator.kt
├── skill/
│   ├── SkillType.kt
│   ├── SkillCategory.kt
│   ├── SkillData.kt
│   ├── PlayerSkillsComponent.kt
│   └── PlayerSkillsComponentCodec.kt
├── domain/
│   ├── xp/
│   │   ├── XpCurve.kt
│   │   └── XpPolicy.kt
│   ├── effects/
│   │   ├── SkillEffectCalculator.kt
│   │   └── SkillEffectPolicy.kt
│   └── resolver/
│       ├── WeaponSkillResolver.kt
│       ├── BlockSkillResolver.kt
│       └── MovementXpPolicy.kt
├── app/
│   ├── SkillRepository.kt               # ECS access boundary
│   ├── XpService.kt
│   ├── DeathPenaltyService.kt
│   └── NotificationService.kt
├── systems/
│   ├── SkillEffectDamageSystem.kt
│   ├── CombatXpDamageSystem.kt
│   └── MovementTickSystem.kt            # native ticking system replacement for Hexweave tick DSL
├── listeners/
│   ├── PlayerLifecycleListener.kt
│   ├── HarvestListener.kt
│   └── OptionalGlobalListeners.kt
└── command/
    └── SkillsCommand.kt
```

---

## Lifecycle Blueprint

## `setup()` responsibilities

- Load and validate config
- Build service graph
- Register component types and codecs
- Register ECS systems
- Register event listeners
- Register commands
- Build `RuntimeState`

## `start()` responsibilities

- Start periodic tasks if needed
- Emit startup diagnostics
- Do not register new structural types here

## `shutdown()` responsibilities

- Persist pending player skill data
- Cancel tasks/listener registrations if retained
- Clear runtime maps and references

---

## Core Boot Flow (Step-by-Step)

1. `SkillsPlugin(JavaPluginInit)` constructed by server.
2. `setup()` invoked by plugin manager.
3. `withConfig("skills", SkillsConfigCodec.CODEC)` created.
4. `config.load().join()` performed and validated.
5. `PlayerSkillsComponent` type registered with codec.
6. Service graph created from config and logger.
7. Damage systems registered with ordering dependencies.
8. Event listeners registered on `EventRegistry`.
9. Commands registered on `CommandRegistry`.
10. `RuntimeState` stored.
11. `start()` invoked when server begins plugin runtime.
12. `shutdown()` flushes all tracked player skills.

---

## Native Registration Patterns

## Plugin entrypoint skeleton

```kotlin
package org.zunkree.hytale.plugins.skillsplugin

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import org.zunkree.hytale.plugins.skillsplugin.bootstrap.PluginApplication

class SkillsPlugin(init: JavaPluginInit) : JavaPlugin(init) {
    private lateinit var app: PluginApplication

    override fun setup() {
        super.setup()
        app = PluginApplication(this)
        app.setup()
    }

    override fun start() {
        super.start()
        app.start()
    }

    override fun shutdown() {
        app.shutdown()
        super.shutdown()
    }
}
```

## Config pattern (native)

```kotlin
val skillsConfigRef = withConfig("skills", SkillsConfigCodec.CODEC)
val skillsConfig = skillsConfigRef.load().join()
SkillsConfigValidator.validate(skillsConfig)
```

## Listener registration pattern

```kotlin
val readyRegistration = eventRegistry.register(PlayerReadyEvent::class.java) { event ->
    playerLifecycleListener.onPlayerReady(event)
}

val disconnectRegistration = eventRegistry.register(PlayerDisconnectEvent::class.java) { event ->
    playerLifecycleListener.onPlayerDisconnect(event)
}
```

## Command registration pattern

```kotlin
commandRegistry.registerCommand(
    SkillsCommand(skillRepository, xpCurve, config.general, logger)
)
```

## Component registration pattern

```kotlin
PlayerSkillsComponent.componentType = entityStoreRegistry.registerComponent(
    PlayerSkillsComponent::class.java,
    "SkillsComponent",
    PlayerSkillsComponentCodec.CODEC,
)
```

---

## Combat Pipeline Design (Native)

Because this server build exposes `DamageEventSystem` and damage module systems, combat effects and XP should be implemented as ordered ECS systems.

## System ordering

- Skill effect mutation system: `Order.BEFORE` `DamageSystems.ApplyDamage`
- Combat XP accounting system: `Order.AFTER` `DamageSystems.ApplyDamage`

## Why this ordering

- BEFORE: modify outgoing damage and stamina drain multipliers prior to final application.
- AFTER: grant XP only for effective, non-cancelled outcomes.

## Guardrails

- Always check `damage.isCancelled` before logic.
- Clamp values before writing `setAmount(...)`.
- Avoid repeated expensive lookups in hot path.

---

## Persistence Design

## Data model

- `PlayerSkillsComponent`
- `Map<SkillType, SkillData>`
- `deathImmunityUntil` timestamp

## Persistence rules

- Read: `store.getComponent(ref, PlayerSkillsComponent.componentType)`
- Create missing: instantiate default component
- Save: `store.putComponent(ref, PlayerSkillsComponent.componentType, component)`

## Serialization

- Keep `BuilderCodec` definitions version-aware.
- Use stable field keys (`SkillType.name`) and do not rename casually.
- For schema evolution, add fields with defaults and explicit migration notes.

---

## Threading and Concurrency Rules

## Golden rules

- ECS mutations only on owning store thread.
- Commands should use `AbstractPlayerCommand` execute context to access store/ref safely.
- Avoid blocking I/O in damage/tick systems.

## Allowed expensive work

- Config load during setup
- Startup diagnostics in start
- Controlled save on shutdown

## Not allowed in hot loops

- Full map scans per tick per player
- String-heavy logging without guard in frequent systems
- Dynamic reflection/lookup in damage handlers

---

## Command Architecture

## Base choice

- Player-facing commands: `AbstractPlayerCommand`
- Background async operations: `AbstractAsyncCommand`

## Permission model

- Use generated or explicit permission nodes from `AbstractCommand`.
- Keep admin commands in a separate command root if added later.

## Output strategy

- Keep `/skills` low-allocation and concise.
- UI command can be layered later without changing core repository/services.

---

## Event Coverage Matrix

| Concern                          | Preferred Integration               | API Type                         |
|----------------------------------|-------------------------------------|----------------------------------|
| Player joins world and data init | Event listener                      | `PlayerReadyEvent`               |
| Player disconnect and save       | Event listener                      | `PlayerDisconnectEvent`          |
| Combat damage modifications      | ECS system                          | `DamageEventSystem` BEFORE apply |
| Combat XP grants                 | ECS system                          | `DamageEventSystem` AFTER apply  |
| Block damage XP                  | Event or ECS event system           | `DamageBlockEvent`               |
| Movement XP/effects              | Native ticking ECS system           | custom `EntityTickingSystem`     |
| Death penalty                    | Damage/death module compatible hook | server damage/death flow         |

---

## Manifest and Dependency Policy

## Mandatory dependencies

- Add server module dependencies explicitly when relying on module-provided systems.
- Keep dependency list minimal and explicit.

## Recommendation

- If combat/death hooks rely on damage module internals, include dependency on the damage module in manifest metadata.

---

## Migration Plan (From Current Project)

## Phase A: Remove framework entrypoint usage

- Replace `KotlinPlugin` inheritance with `JavaPlugin`.
- Remove DSL-based event/command registration calls.
- Keep existing domain classes unchanged where possible.

## Phase B: Replace Hexweave systems

- Re-implement movement and block hooks as native ECS systems/events.
- Preserve behavior parity first, optimize second.

## Phase C: Native config bootstrap

- Replace `jsonConfig` with `withConfig(...).load()` workflow.
- Keep existing config schema and validation logic.

## Phase D: Hardening

- Add startup assertions for critical registrations.
- Add integration checklist for setup/start/shutdown and player reconnect persistence.

---

## Testing Strategy

## Unit tests

- Keep domain math and policy tests (XP curve, effect scaling, resolver logic).

## Integration checks (manual or harness)

- Fresh player joins -> component exists with defaults
- Combat damage modifies output before apply
- Combat XP grants after apply
- Disconnect/restart preserves skills
- Death penalty respects immunity window

## Performance checks

- 20+ concurrent players combat/movement load
- No repeated warning/error logs in hot loops

---

## Risk Register

| Risk                                                | Impact | Mitigation                                                            |
|-----------------------------------------------------|--------|-----------------------------------------------------------------------|
| Missing equivalent for Hexweave tick/event features | Medium | Implement native ECS tick systems incrementally and verify parity     |
| Thread misuse in store mutations                    | High   | Restrict mutations to command/event/system contexts with store access |
| Codec key changes break persistence                 | High   | Freeze keys, add migration version plan                               |
| Over-logging in damage systems                      | Medium | Reduce log level and guard debug statements                           |
| Dependency ordering mistakes in combat systems      | High   | Explicit `SystemDependency` tests and startup assertions              |

---

## ADHD-Friendly Execution Checklist

## Week 1: Foundation

- [ ] Switch plugin base to `JavaPlugin`
- [ ] Introduce `PluginApplication` and `RuntimeState`
- [ ] Port config loading to native `Config<T>`
- [ ] Register `PlayerSkillsComponent` natively

## Week 2: Runtime paths

- [ ] Port command registration to native registry
- [ ] Port player lifecycle listeners to native event registry
- [ ] Keep combat systems on native `DamageEventSystem`

## Week 3: Remove wrappers

- [ ] Replace Hexweave movement tick with native ticking system
- [ ] Remove Kytale-specific imports/utilities
- [ ] Remove wrapper dependencies from build and manifest

## Week 4: Stabilize

- [ ] Full reconnect/persistence verification
- [ ] Hot-path profiling and log tuning
- [ ] Documentation updates and contributor migration notes

---

## Definition of Done

The migration is complete when all statements are true:

- Plugin compiles and runs with no Kytale/Hexweave runtime dependency.
- Setup/start/shutdown are fully native and deterministic.
- Skill progression persists correctly across server restart.
- Combat and movement effects are applied through native systems only.
- Commands and listeners are registered through native registries only.
- No core gameplay path depends on wrapper-specific DSL helpers.

---

## References

- Local API decompilation: `HytaleServer.jar` (release patchline in local install)
- Hytale plugin development reference: https://www.hytale-dev.com/plugin-development
- Lifecycle page: https://www.hytale-dev.com/plugin-development/lifecycle
- ECS architecture page: https://www.hytale-dev.com/plugin-development/ecs-architecture
- Event system page: https://www.hytale-dev.com/plugin-development/event-system
- Commands page: https://www.hytale-dev.com/plugin-development/commands
- Codecs page: https://www.hytale-dev.com/plugin-development/codecs
- Thread safety page: https://www.hytale-dev.com/plugin-development/thread-safety
