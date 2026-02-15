package org.zunkree.hytale.plugins.skillsplugin.config

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

object SkillsConfigCodec {
    private val GENERAL_CODEC: BuilderCodec<GeneralConfig> =
        BuilderCodec
            .builder(GeneralConfig::class.java, ::GeneralConfig)
            .append(KeyedCodec("MaxLevel", Codec.INTEGER), { obj, v -> obj.maxLevel = v }, { it.maxLevel })
            .add()
            .append(
                KeyedCodec("ShowLevelUpNotifications", Codec.BOOLEAN),
                { obj, v -> obj.showLevelUpNotifications = v },
                { it.showLevelUpNotifications },
            ).add()
            .append(
                KeyedCodec("ShowXpGainNotifications", Codec.BOOLEAN),
                { obj, v -> obj.showXpGainNotifications = v },
                { it.showXpGainNotifications },
            ).add()
            .build()

    private val ACTION_XP_CODEC: BuilderCodec<ActionXpConfig> =
        BuilderCodec
            .builder(ActionXpConfig::class.java, ::ActionXpConfig)
            .append(
                KeyedCodec("CombatDamageMultiplier", Codec.DOUBLE),
                { obj, v -> obj.combatDamageMultiplier = v },
                { it.combatDamageMultiplier },
            ).add()
            .append(
                KeyedCodec("MiningPerBlockMultiplier", Codec.DOUBLE),
                { obj, v -> obj.miningPerBlockMultiplier = v },
                { it.miningPerBlockMultiplier },
            ).add()
            .append(
                KeyedCodec("WoodcuttingPerBlockMultiplier", Codec.DOUBLE),
                { obj, v -> obj.woodcuttingPerBlockMultiplier = v },
                { it.woodcuttingPerBlockMultiplier },
            ).add()
            .append(
                KeyedCodec("RunningPerDistanceMultiplier", Codec.DOUBLE),
                { obj, v -> obj.runningPerDistanceMultiplier = v },
                { it.runningPerDistanceMultiplier },
            ).add()
            .append(
                KeyedCodec("SwimmingPerDistanceMultiplier", Codec.DOUBLE),
                { obj, v -> obj.swimmingPerDistanceMultiplier = v },
                { it.swimmingPerDistanceMultiplier },
            ).add()
            .append(
                KeyedCodec("SneakingPerSecondMultiplier", Codec.DOUBLE),
                { obj, v -> obj.sneakingPerSecondMultiplier = v },
                { it.sneakingPerSecondMultiplier },
            ).add()
            .append(
                KeyedCodec("JumpingPerJumpMultiplier", Codec.DOUBLE),
                { obj, v -> obj.jumpingPerJumpMultiplier = v },
                { it.jumpingPerJumpMultiplier },
            ).add()
            .append(
                KeyedCodec("BlockingDamageMultiplier", Codec.DOUBLE),
                { obj, v -> obj.blockingDamageMultiplier = v },
                { it.blockingDamageMultiplier },
            ).add()
            .append(
                KeyedCodec("DivingPerSecondMultiplier", Codec.DOUBLE),
                { obj, v -> obj.divingPerSecondMultiplier = v },
                { it.divingPerSecondMultiplier },
            ).add()
            .build()

    private val XP_CODEC: BuilderCodec<XpConfig> =
        BuilderCodec
            .builder(XpConfig::class.java, ::XpConfig)
            .append(
                KeyedCodec("BaseXpPerAction", Codec.DOUBLE),
                { obj, v -> obj.baseXpPerAction = v },
                { it.baseXpPerAction },
            ).add()
            .append(
                KeyedCodec("XpScaleFactor", Codec.DOUBLE),
                { obj, v -> obj.xpScaleFactor = v },
                { it.xpScaleFactor },
            ).add()
            .append(
                KeyedCodec("RestedBonusMultiplier", Codec.DOUBLE),
                { obj, v -> obj.restedBonusMultiplier = v },
                { it.restedBonusMultiplier },
            ).add()
            .append(
                KeyedCodec("GlobalXpMultiplier", Codec.DOUBLE),
                { obj, v -> obj.globalXpMultiplier = v },
                { it.globalXpMultiplier },
            ).add()
            .append(
                KeyedCodec("ActionXp", ACTION_XP_CODEC),
                { obj, v -> obj.actionXp = v },
                { it.actionXp },
            ).add()
            .build()

    private val DEATH_PENALTY_CODEC: BuilderCodec<DeathPenaltyConfig> =
        BuilderCodec
            .builder(DeathPenaltyConfig::class.java, ::DeathPenaltyConfig)
            .append(
                KeyedCodec("Enabled", Codec.BOOLEAN),
                { obj, v -> obj.enabled = v },
                { it.enabled },
            ).add()
            .append(
                KeyedCodec("PenaltyPercentage", Codec.DOUBLE),
                { obj, v -> obj.penaltyPercentage = v },
                { it.penaltyPercentage },
            ).add()
            .append(
                KeyedCodec("ImmunityDurationSeconds", Codec.INTEGER),
                { obj, v -> obj.immunityDurationSeconds = v },
                { it.immunityDurationSeconds },
            ).add()
            .append(
                KeyedCodec("ShowImmunityInHud", Codec.BOOLEAN),
                { obj, v -> obj.showImmunityInHud = v },
                { it.showImmunityInHud },
            ).add()
            .build()

    private val SKILL_EFFECT_ENTRY_CODEC: BuilderCodec<SkillEffectEntry> =
        BuilderCodec
            .builder(SkillEffectEntry::class.java, ::SkillEffectEntry)
            .append(
                KeyedCodec("MinDamage", Codec.FLOAT),
                { obj, v -> obj.minDamage = v },
                { it.minDamage },
            ).add()
            .append(
                KeyedCodec("MaxDamage", Codec.FLOAT),
                { obj, v -> obj.maxDamage = v },
                { it.maxDamage },
            ).add()
            .append(
                KeyedCodec("MinSpeed", Codec.FLOAT),
                { obj, v -> obj.minSpeed = v },
                { it.minSpeed },
            ).add()
            .append(
                KeyedCodec("MaxSpeed", Codec.FLOAT),
                { obj, v -> obj.maxSpeed = v },
                { it.maxSpeed },
            ).add()
            .append(
                KeyedCodec("MinSpeedBonus", Codec.FLOAT),
                { obj, v -> obj.minSpeedBonus = v },
                { it.minSpeedBonus },
            ).add()
            .append(
                KeyedCodec("MaxSpeedBonus", Codec.FLOAT),
                { obj, v -> obj.maxSpeedBonus = v },
                { it.maxSpeedBonus },
            ).add()
            .append(
                KeyedCodec("MinStaminaReduction", Codec.FLOAT),
                { obj, v -> obj.minStaminaReduction = v },
                { it.minStaminaReduction },
            ).add()
            .append(
                KeyedCodec("MaxStaminaReduction", Codec.FLOAT),
                { obj, v -> obj.maxStaminaReduction = v },
                { it.maxStaminaReduction },
            ).add()
            .append(
                KeyedCodec("MinHeight", Codec.FLOAT),
                { obj, v -> obj.minHeight = v },
                { it.minHeight },
            ).add()
            .append(
                KeyedCodec("MaxHeight", Codec.FLOAT),
                { obj, v -> obj.maxHeight = v },
                { it.maxHeight },
            ).add()
            .append(
                KeyedCodec("MinOxygenBonus", Codec.FLOAT),
                { obj, v -> obj.minOxygenBonus = v },
                { it.minOxygenBonus },
            ).add()
            .append(
                KeyedCodec("MaxOxygenBonus", Codec.FLOAT),
                { obj, v -> obj.maxOxygenBonus = v },
                { it.maxOxygenBonus },
            ).add()
            .build()

    val CODEC: BuilderCodec<SkillsConfig> =
        run {
            val builder =
                BuilderCodec
                    .builder(SkillsConfig::class.java, ::SkillsConfig)
                    .append(
                        KeyedCodec("General", GENERAL_CODEC),
                        { obj, v -> obj.general = v },
                        { it.general },
                    ).add()
                    .append(
                        KeyedCodec("Xp", XP_CODEC),
                        { obj, v -> obj.xp = v },
                        { it.xp },
                    ).add()
                    .append(
                        KeyedCodec("DeathPenalty", DEATH_PENALTY_CODEC),
                        { obj, v -> obj.deathPenalty = v },
                        { it.deathPenalty },
                    ).add()

            // Encode each skill effect as a top-level key under "SkillEffects"
            for (skill in SkillType.entries) {
                builder
                    .append(
                        KeyedCodec("SkillEffects_${skill.name}", SKILL_EFFECT_ENTRY_CODEC),
                        { obj, v ->
                            val mutable = obj.skillEffects.toMutableMap()
                            mutable[skill] = v
                            obj.skillEffects = mutable
                        },
                        { it.skillEffects[skill] ?: SkillEffectEntry() },
                    ).add()
            }

            builder.build()
        }
}
