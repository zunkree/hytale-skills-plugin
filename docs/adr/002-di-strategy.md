# ADR-002: Dependency Injection Strategy

## Status

Accepted

## Context

The initial implementation used three `object` singletons (`SkillsXpCalculator`, `SkillsManager`, `SkillsXpService`) that accessed configuration and the logger via `SkillsPlugin.instance`. This created 26 references to `SkillsPlugin.instance` across 9 files, making unit testing impossible and coupling every component to the plugin lifecycle.

Options considered:

1. **DI framework** (Koin, Dagger) — adds a dependency, complex for a 18-file plugin.
2. **Service locator** — hides dependencies, makes testing slightly easier but still couples to a global registry.
3. **Manual constructor injection** — explicit, no dependencies, fully testable.

## Decision

Use **manual constructor injection** with `SkillsPlugin.setup()` as the sole composition root.

## Implementation

All former singletons became classes with constructor parameters:

```kotlin
class XpCurve(private val xpConfig: XpConfig)
class SkillRepository(private val logger: HytaleLogger)
class XpService(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val generalConfig: GeneralConfig,
    private val logger: HytaleLogger,
)
```

`SkillsPlugin.setup()` wires everything:

```kotlin
val xpCurve = XpCurve(config.xp)
val skillRepository = SkillRepository(logger)
val xpService = XpService(skillRepository, xpCurve, config.general, logger)
// ... create listeners, command handler, register events
```

## Consequences

- `SkillsPlugin.instance` has been removed entirely.
- Pure domain classes (`XpCurve`, resolvers, policies) are testable with zero mocking.
- Adding a new dependency to a class is a compiler error at the composition root — impossible to forget wiring.
- No framework dependency for DI. No runtime overhead.
- Trade-off: constructor parameter lists grow as dependencies increase. Acceptable at this project size.
