# How to Add a New Skill

This guide walks through adding a new skill to the plugin, using a hypothetical "Fishing" skill as an example.

## 1. Add the Enum Constant

In `skill/SkillType.kt`, add a new entry:

```kotlin
enum class SkillType(val displayName: String, val category: SkillCategory) {
    // ... existing skills ...
    FISHING("Fishing", SkillCategory.GATHERING),
}
```

This is the only change needed for the skill to appear in save data and the `/skills` command.

## 2. Define XP Triggers

Decide how the player earns XP. For fishing, it might be a "catch fish" event.

### Option A: Simple Event Mapping

If XP is granted from a single event type, add logic directly in an existing or new listener:

```kotlin
class FishingListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val logger: HytaleLogger,
) {
    fun onFishCaught(event: FishCaughtEvent) {
        val xpGain = actionXpConfig.fishingXpPerCatch // add to ActionXpConfig
        xpService.grantXp(event.player, SkillType.FISHING, xpGain)
    }
}
```

### Option B: Complex Resolution Logic

If the XP amount depends on context (e.g., fish type, tool used), create a pure resolver:

```kotlin
// resolver/FishingXpPolicy.kt
class FishingXpPolicy(private val config: ActionXpConfig) {
    fun calculateXp(fishType: String, toolQuality: Int): Double {
        // Pure logic, no framework imports
    }
}
```

Keep the resolver free of framework imports so it's directly testable.

## 3. Add Configuration

In `config/SkillsConfig.kt`, add the XP value to `ActionXpConfig`:

```kotlin
data class ActionXpConfig(
    // ... existing fields ...
    var fishingXpPerCatch: Double = 5.0,
)
```

Also add the corresponding codec field in `SkillsConfigCodec`:

```kotlin
.append(
    KeyedCodec("FishingXpPerCatch", Codec.DOUBLE),
    { obj, v -> obj.fishingXpPerCatch = v },
    { it.fishingXpPerCatch },
).add()
```

## 4. Wire It Up

In `PluginApplication`, create the listener and register it:

```kotlin
// In PluginApplication.createServices():
val fishingListener = FishingListener(xpService, config.xp.actionXp, logger)

// In PluginApplication.registerAll():
plugin.eventRegistry.register(FishCaughtEvent::class.java) { event ->
    fishingListener.onFishCaught(event)
}
```

## 5. Add Skill Effects (Optional)

If the skill should provide gameplay bonuses, add an effect entry in the config:

```json
{
    "skillEffects": {
        "FISHING": {
            "minEffect": 0.0,
            "maxEffect": 0.5,
            "description": "Increased catch rate"
        }
    }
}
```

Effect application depends on the Hytale API for the specific mechanic.

## 6. Write Tests

Test any pure logic you added:

```kotlin
class FishingXpPolicyTest {
    @Test
    fun `grants base XP for common fish`() {
        val policy = FishingXpPolicy(ActionXpConfig())
        val xp = policy.calculateXp("CommonFish", toolQuality = 1)
        assertEquals(5.0, xp)
    }
}
```

## 7. Validate

```bash
./gradlew ktlintCheck detekt test
```

## Checklist

- [ ] Enum constant added to `SkillType`
- [ ] XP trigger identified and implemented (listener + optional resolver)
- [ ] Configuration value added to `ActionXpConfig`
- [ ] Listener wired in `PluginApplication`
- [ ] Tests written for any pure domain logic
- [ ] Quality checks pass
