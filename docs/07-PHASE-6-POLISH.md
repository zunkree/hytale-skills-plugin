# Phase 6 — Polish & Release

## Goal
Finalize configuration options, add admin commands, optimize performance, and prepare for CurseForge release.

## Prerequisites
- Phase 5 complete (UI working)

## Done Criteria
- [ ] Config hot-reload command for admins
- [ ] Admin commands: `/skills set`, `/skills reset`, `/skills give` with permission checks
- [ ] Performance optimized (caching, efficient event handling)
- [ ] Comprehensive logging for debugging
- [ ] README with installation and configuration guide
- [ ] CurseForge page with screenshots and description
- [ ] Version 1.0.0 released

---

## Tasks

### Task 6.1 — Add admin commands

Uses `AbstractCommandCollection` for subcommand routing and `AbstractPlayerCommand` for thread-safe ECS access. Argument types use `ArgTypes` from Hytale's command API.

> **Note:** Config data classes (`SkillsConfig`, etc.) and `config.json` were established in Phase 1.5. This phase adds admin commands for runtime management.

```kotlin
// Admin command collection: /skills admin <set|reset|give|reload>
class AdminCommandCollection(
    private val skillRepository: SkillRepository,
    private val xpService: XpService,
    private val xpCurve: XpCurve
) : AbstractCommandCollection("admin", "Admin commands") {

    override fun register() {
        sub("set", SetSkillCommand(skillRepository, xpCurve))
        sub("reset", ResetSkillsCommand(skillRepository))
        sub("give", GiveXpCommand(xpService))
        sub("reload", ReloadCommand())
    }
}

// /skills admin set <player> <skill> <level>
class SetSkillCommand(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve
) : AbstractPlayerCommand() {

    override fun buildArgs(builder: CommandBase.ArgsBuilder) {
        builder.required("player", ArgTypes.PLAYER)
        builder.required("skill", ArgTypes.STRING)  // SkillType.name
        builder.required("level", ArgTypes.INTEGER)
    }

    override fun execute(ctx: PlayerCommandContext) {
        val targetRef = ctx.getPlayerRef("player")
        val skillName = ctx.getString("skill").uppercase()
        val level = ctx.getInt("level").coerceIn(0, 100)

        val skillType = SkillType.entries.find { it.name == skillName } ?: run {
            ctx.sendMessage(Message.raw("Unknown skill: $skillName"))
            return
        }

        val skills = skillRepository.getPlayerSkills(targetRef) ?: return
        val skillData = skills.getSkill(skillType)
        skillData.level = level
        skillData.totalXP = xpCurve.cumulativeXpForLevel(level)
        skillRepository.savePlayerSkills(targetRef, skills)

        ctx.sendMessage(Message.raw("Set ${skillType.displayName} to level $level"))
    }
}

// /skills admin reset <player>
class ResetSkillsCommand(
    private val skillRepository: SkillRepository
) : AbstractPlayerCommand() {

    override fun buildArgs(builder: CommandBase.ArgsBuilder) {
        builder.required("player", ArgTypes.PLAYER)
    }

    override fun execute(ctx: PlayerCommandContext) {
        val targetRef = ctx.getPlayerRef("player")
        val skills = PlayerSkillsComponent()  // Fresh component, all skills at 0
        skillRepository.savePlayerSkills(targetRef, skills)
        ctx.sendMessage(Message.raw("Reset all skills"))
    }
}

// /skills admin give <player> <skill> <xp>
class GiveXpCommand(
    private val xpService: XpService
) : AbstractPlayerCommand() {

    override fun buildArgs(builder: CommandBase.ArgsBuilder) {
        builder.required("player", ArgTypes.PLAYER)
        builder.required("skill", ArgTypes.STRING)
        builder.required("xp", ArgTypes.DOUBLE)
    }

    override fun execute(ctx: PlayerCommandContext) {
        val targetRef = ctx.getPlayerRef("player")
        val skillName = ctx.getString("skill").uppercase()
        val xp = ctx.getDouble("xp")

        val skillType = SkillType.entries.find { it.name == skillName } ?: run {
            ctx.sendMessage(Message.raw("Unknown skill: $skillName"))
            return
        }

        xpService.grantXp(targetRef, skillType, xp)
        ctx.sendMessage(Message.raw("Gave $xp XP in ${skillType.displayName}"))
    }
}

// /skills admin reload
class ReloadCommand(private val configRef: Config<SkillsConfig>) : CommandBase() {
    override fun execute(ctx: CommandContext) {
        // Reload config from disk via native Config<T> API
        configRef.load().thenAccept { newConfig ->
            // Update references to reloaded config
            ctx.sendMessage(Message.raw("Configuration reloaded"))
        }
    }
}
```

> **Architecture:** Admin commands use `AbstractPlayerCommand` (thread-safe ECS access) and `AbstractCommandCollection` (subcommand routing). `ArgTypes.PLAYER`, `ArgTypes.STRING`, `ArgTypes.INTEGER`, `ArgTypes.DOUBLE` are validated Hytale argument types. Permission checks TBD (Hytale's permission system needs further research).

### Task 6.2 — Performance optimization

```kotlin
// Cache computed effect multipliers to avoid recalculating every tick
class SkillEffectCache(
    private val effectCalculator: SkillEffectCalculator
) {
    private val cache = ConcurrentHashMap<UUID, CachedEffects>()

    data class CachedEffects(
        val multipliers: Map<SkillType, Double>,
        val lastUpdate: Long
    )

    fun getOrCompute(playerId: UUID, skills: PlayerSkillsComponent): CachedEffects {
        val cached = cache[playerId]
        if (cached != null && System.currentTimeMillis() - cached.lastUpdate < 5000L) {
            return cached  // Cache valid for 5 seconds
        }

        val multipliers = SkillType.entries.associate { type ->
            type to effectCalculator.getDamageMultiplier(type, skills.getSkill(type).level)
        }
        val entry = CachedEffects(multipliers, System.currentTimeMillis())
        cache[playerId] = entry
        return entry
    }

    fun invalidate(playerId: UUID) = cache.remove(playerId)
    fun clear() = cache.clear()
}
```

> **Note:** Movement XP throttling is already handled by `MovementListener.cooldowns` (Phase 2). The `SkillEffectCache` here caches computed multipliers to avoid recalculating from config every tick.

### Task 6.3 — Create README.md

```markdown
# skillsplugin

A Valheim-inspired skill progression system for Hytale.

## Features

- **15 Skills**: Swords, Daggers, Axes, Bows, Spears, Clubs, Unarmed, Blocking, Mining, Woodcutting, Running, Swimming, Diving, Sneaking, Jumping
- **Learn by Doing**: Gain XP by performing actions - fight to improve combat skills, run to improve running
- **Meaningful Progression**: Higher skills provide real bonuses - up to 2x damage, faster movement, reduced stamina
- **Death Penalty**: Lose 10% of skill levels on death (configurable), with 5-minute immunity after dying
- **Full UI**: Beautiful skills menu showing all your progress

## Installation

1. Download skillsplugin JAR
2. Place in your Hytale mods folder
3. Launch game

## Commands

- `/skills` - Open skills menu
- `/skills text` - Show skills as text
- `/skills immunity` - Check death immunity status

### Admin Commands
- `/skills admin set <player> <skill> <level>` - Set skill level
- `/skills admin reset <player>` - Reset all skills
- `/skills admin give <player> <skill> <xp>` - Give XP
- `/skills admin reload` - Reload config

## Configuration

Edit `config/skillsplugin/config.json` to customize:
- XP rates
- Death penalty settings
- Skill effect multipliers
- And more!

## License

MIT License
```

---

## Validated APIs

- [x] **AbstractPlayerCommand** — Thread-safe ECS access for admin commands that modify skill data
- [x] **AbstractCommandCollection** — Subcommand routing for `/skills admin <set|reset|give|reload>`
- [x] **ArgTypes** — `ArgTypes.PLAYER`, `ArgTypes.STRING`, `ArgTypes.INTEGER`, `ArgTypes.DOUBLE` for command arguments
- [x] **CommandBase** — Base class for non-player commands (like reload)
- [x] **Config<T>.load()** — Re-reads config from disk for runtime reload

### Research Still Needed

- [ ] **Permission system** — How Hytale handles permissions (operator status, permission nodes, `hasPermission()`)
- [ ] **Permission gating** — How to restrict admin subcommands to operators/admins

---

## Release Checklist

- [ ] All features implemented and tested
- [ ] Config file documented
- [ ] README complete
- [ ] Version bumped to 1.0.0
- [ ] Build produces clean JAR
- [ ] Tested on clean Hytale installation
- [ ] Screenshots taken for CurseForge
- [ ] CurseForge page created with description
- [ ] JAR uploaded to CurseForge
- [ ] Announce on HytaleModding Discord

---

## CurseForge Description Template

```
# skillsplugin - Valheim-Style Progression

Learn by doing! skillsplugin adds a Valheim-inspired skill system where you improve at activities by performing them.

## ⚔️ Features

🗡️ **15 Skills** covering combat, gathering, and movement
📈 **Level 0-100** with meaningful bonuses at each level
💀 **Death Penalty** - lose 10% on death (with 5-min immunity)
🎨 **Beautiful UI** to track your progress

## 📊 Skills

**Combat**: Swords, Daggers, Axes, Bows, Spears, Clubs, Unarmed, Blocking
**Gathering**: Mining, Woodcutting
**Movement**: Running, Swimming, Diving, Sneaking, Jumping

## 🎮 How It Works

- Swing a sword → gain Swords XP
- Chop a tree → gain Woodcutting XP
- Run around → gain Running XP
- Level up for bonuses (up to 2x damage, 50% faster gathering, etc.)

## ⚠️ Death Penalty

Like Valheim, dying has consequences! You'll lose 10% of your skill levels, but you get 5 minutes of immunity to prevent frustrating death spirals.

## 📋 Requirements

- Hytale server (no additional dependencies required)
```
