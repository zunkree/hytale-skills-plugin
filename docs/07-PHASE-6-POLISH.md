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

> **Note:** Config data classes (`SkillsConfig`, etc.) and `config.json` were established in Phase 1.5. This phase adds admin commands for runtime management.

```kotlin
// TODO: Research actual Hytale/Kytale command argument types —
// PlayerArgument, EnumArgument, IntArgument, FloatArgument are pseudo-code
command("skills", "Skill commands") {
    // ... existing subcommands ...

    subcommand("admin", "Admin commands") {
        // Permission check — reject non-admins before processing subcommands
        requires { ctx ->
            // TODO: Research actual Hytale permission API
            ctx.hasPermission(HytaleSkillsPlugin.instance.config.permissions.adminCommandsPermission)
        }

        subcommand("set", "Set a player's skill level") {
            // /skills admin set <player> <skill> <level>
            argument("player", PlayerArgument)       // pseudo-code argument type
            argument("skill", EnumArgument(SkillType::class))  // pseudo-code argument type
            argument("level", IntArgument(0, 100))   // pseudo-code argument type

            executes { ctx ->
                val target = ctx.get<Player>("player")
                val skillType = ctx.get<SkillType>("skill")
                val level = ctx.get<Int>("level")

                val playerRef = target.asPlayerRef()
                val skills = SkillManager.getPlayerSkills(playerRef) ?: return@executes
                val skillData = skills.getSkill(skillType)

                skillData.level = level
                skillData.totalXp = XpCalculator.cumulativeXpForLevel(level)
                SkillManager.savePlayerSkills(playerRef, skills)

                ctx.sendMessage(Message.raw("Set ${target.name}'s ${skillType.displayName} to level $level"))
            }
        }

        subcommand("reset", "Reset a player's skills") {
            argument("player", PlayerArgument)  // pseudo-code argument type

            executes { ctx ->
                val target = ctx.get<Player>("player")
                val playerRef = target.asPlayerRef()

                // Reset all skills to 0
                val skills = PlayerSkillsComponent()
                SkillManager.savePlayerSkills(playerRef, skills)

                ctx.sendMessage(Message.raw("Reset all skills for ${target.name}"))
            }
        }

        subcommand("give", "Give XP to a player") {
            argument("player", PlayerArgument)       // pseudo-code argument type
            argument("skill", EnumArgument(SkillType::class))  // pseudo-code argument type
            argument("xp", FloatArgument(0f, 100000f))  // pseudo-code argument type

            executes { ctx ->
                val target = ctx.get<Player>("player")
                val skillType = ctx.get<SkillType>("skill")
                val xp = ctx.get<Float>("xp")

                val playerRef = target.asPlayerRef()
                SkillXpService.grantXp(playerRef, skillType, xp)

                ctx.sendMessage(Message.raw("Gave $xp XP to ${target.name} in ${skillType.displayName}"))
            }
        }

        subcommand("reload", "Reload configuration") {
            executes { ctx ->
                // Reload config
                HytaleSkillsPlugin.instance.reloadConfig()
                ctx.sendMessage(Message.raw("Configuration reloaded"))
            }
        }
    }
}
```

### Task 6.2 — Performance optimization

```kotlin
// Cache skill data for active players
object SkillCache {
    private val cache = ConcurrentHashMap<UUID, CachedSkills>()

    data class CachedSkills(
        val skills: PlayerSkillsComponent,
        val effectMultipliers: Map<SkillType, Float>,
        val lastUpdate: Long
    )

    fun get(playerId: UUID): CachedSkills? = cache[playerId]

    fun update(playerId: UUID, skills: PlayerSkillsComponent) {
        val multipliers = SkillType.values().associate { type ->
            type to SkillEffects.getConfig(type).getMultiplierAtLevel(skills.getSkill(type).level)
        }
        cache[playerId] = CachedSkills(skills, multipliers, System.currentTimeMillis())
    }

    fun invalidate(playerId: UUID) {
        cache.remove(playerId)
    }

    fun clear() {
        cache.clear()
    }
}

// Throttle movement XP checks
object MovementThrottle {
    private val lastCheck = ConcurrentHashMap<UUID, Long>()
    private const val CHECK_INTERVAL_MS = 100L // Only check every 100ms

    fun shouldProcess(playerId: UUID): Boolean {
        val now = System.currentTimeMillis()
        val last = lastCheck[playerId] ?: 0L
        if (now - last >= CHECK_INTERVAL_MS) {
            lastCheck[playerId] = now
            return true
        }
        return false
    }
}
```

### Task 6.3 — Create README.md

```markdown
# skillsplugin

A Valheim-inspired skill progression system for Hytale.

## Features

- **13 Skills**: Swords, Axes, Bows, Spears, Clubs, Unarmed, Blocking, Mining, Woodcutting, Running, Swimming, Sneaking, Jumping
- **Learn by Doing**: Gain XP by performing actions - fight to improve combat skills, run to improve running
- **Meaningful Progression**: Higher skills provide real bonuses - up to 2x damage, faster movement, reduced stamina
- **Death Penalty**: Lose 5% of skill levels on death (configurable), with 10-minute immunity after dying
- **Full UI**: Beautiful skills menu showing all your progress

## Installation

1. Install [Kytale](https://curseforge.com/hytale/mods/kytale) (required dependency)
2. Download skillsplugin JAR
3. Place in your Hytale mods folder
4. Launch game

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

## Research Required

Before implementing this phase, confirm:

- [ ] **Permission system** — How Hytale handles permissions (`hasPermission()`, permission nodes, operator status)
- [ ] **Command argument types** — Actual Kytale DSL for player selectors, enum arguments, numeric arguments
- [ ] **Config reload** — Whether Kytale `jsonConfig` supports runtime reload or requires custom implementation
- [ ] **`requires` predicate** — How to gate subcommands behind permission checks in the Kytale command DSL

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

🗡️ **13 Skills** covering combat, gathering, and movement
📈 **Level 0-100** with meaningful bonuses at each level
💀 **Death Penalty** - lose 5% on death (with immunity protection)
🎨 **Beautiful UI** to track your progress

## 📊 Skills

**Combat**: Swords, Axes, Bows, Spears, Clubs, Unarmed, Blocking
**Gathering**: Mining, Woodcutting
**Movement**: Running, Swimming, Sneaking, Jumping

## 🎮 How It Works

- Swing a sword → gain Swords XP
- Chop a tree → gain Woodcutting XP
- Run around → gain Running XP
- Level up for bonuses (up to 2x damage, 50% faster gathering, etc.)

## ⚠️ Death Penalty

Like Valheim, dying has consequences! You'll lose 5% of your skill levels, but you get 10 minutes of immunity to prevent frustrating death spirals.

## 📋 Requirements

- Kytale (required)
```
