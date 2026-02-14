# ADR-001: Persistence Key Strategy

## Status

Accepted

## Context

`PlayerSkillsComponent` serializes skill data using `KeyedCodec`. Each skill entry needs a stable string key for the ECS persistence layer. Two candidates:

1. **`skill.displayName`** — Human-readable name (e.g., "Swords", "Wood Cutting")
2. **`skill.name`** — Enum constant name (e.g., "SWORDS", "WOODCUTTING")

## Decision

Use **`skill.name`** (enum constant) as the CODEC key.

## Rationale

- Enum constant names are compile-time stable. Renaming one is a deliberate, visible change that triggers compiler errors at all usage sites.
- Display names are presentation concerns that may change for localization, typo fixes, or style preferences. Changing a display name should never corrupt persisted data.
- `skill.name` is guaranteed unique by the Kotlin compiler (no two enum constants can share a name).

## Consequences

- Persisted keys will be uppercase: `"SWORDS"`, `"WOODCUTTING"`, `"RUNNING"`.
- Renaming an enum constant is a breaking change for existing save data — this is intentional and desirable (forces migration consideration).
- Display name changes are safe and require no data migration.
