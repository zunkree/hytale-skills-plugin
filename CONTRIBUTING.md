# Contributing

## Development Setup

1. Install Java 25 and Hytale via the official launcher.
2. Clone the repository.
3. Run `./gradlew build` to compile (requires Hytale and Kytale JARs).
4. Run `./gradlew test` to run unit tests (no Hytale installation required).

## Coding Standards

- **Kotlin**: Follow ktlint defaults. Max line length is 120 characters.
- **No wildcard imports**: Use explicit imports only.
- **Constructor injection**: Never access `SkillsPlugin.instance` outside `SkillsPlugin.kt`. Pass dependencies via constructors.
- **Pure domain logic**: Resolvers and policies must have zero framework imports. If you need framework types, keep them in the listener layer.
- **Naming**: Use `SkillType` (not `SkillsType`), `SkillData` (not `SkillsData`). Drop the `Skills` prefix unless the class is the plugin itself.

## Quality Checks

All changes must pass before merge:

```bash
./gradlew ktlintCheck detekt test koverVerify
```

- **ktlint**: Code formatting
- **detekt**: Static analysis (see `detekt.yml`)
- **tests**: JUnit 5 unit tests
- **kover**: 50% minimum code coverage

## Testing Guidelines

- Test pure domain logic directly (resolvers, policies, XP calculations).
- Framework-dependent code (listeners, ECS operations) cannot be unit tested — keep these layers thin.
- Use descriptive test names with backtick syntax: `` `grants running XP when running with distance`() ``.
- Place tests in the same package as the code under test.

## Project Structure

See [README.md](README.md) for the full module map. Key conventions:

| Package | Contains | Framework deps? |
|---------|----------|-----------------|
| `resolver/` | Pure mapping logic | No |
| `xp/` | XP math and service | `XpCurve` no, `XpService` yes |
| `listener/` | Event adapters | Yes |
| `persistence/` | ECS read/write | Yes |
| `config/` | Data classes | No (kotlinx-serialization only) |

## Review Expectations

- PRs should include tests for any new pure domain logic.
- Lint and detekt must pass — CI will block on failures.
- Keep listeners thin: extract logic into resolver/policy classes.
- Config changes need corresponding validation in `SkillsPlugin.validateConfig()`.
