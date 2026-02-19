package org.zunkree.hytale.plugins.skillsplugin.config

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

data class SkillsConfig(
    var general: GeneralConfig = GeneralConfig(),
    var xp: XpConfig = XpConfig(),
    var deathPenalty: DeathPenaltyConfig = DeathPenaltyConfig(),
    var skillEffects: Map<SkillType, SkillEffectEntry> = defaultSkillEffects(),
) {
    companion object {
        fun defaultSkillEffects(): Map<SkillType, SkillEffectEntry> =
            mapOf(
                SkillType.SWORDS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.AXES to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.DAGGERS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.BOWS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.SPEARS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.CLUBS to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.UNARMED to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
                SkillType.BLOCKING to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.5f),
                SkillType.MINING to SkillEffectEntry(minSpeed = 1.0f, maxSpeed = 1.5f),
                SkillType.WOODCUTTING to SkillEffectEntry(minSpeed = 1.0f, maxSpeed = 1.5f),
                SkillType.RUNNING to
                    SkillEffectEntry(
                        minSpeedBonus = 0.0f,
                        maxSpeedBonus = 0.25f,
                        minStaminaReduction = 0.0f,
                        maxStaminaReduction = 0.33f,
                    ),
                SkillType.SWIMMING to
                    SkillEffectEntry(
                        minSpeedBonus = 0.0f,
                        maxSpeedBonus = 0.25f,
                        minStaminaReduction = 0.0f,
                        maxStaminaReduction = 0.33f,
                    ),
                SkillType.DIVING to SkillEffectEntry(minOxygenBonus = 0.0f, maxOxygenBonus = 1.0f),
                SkillType.SNEAKING to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.75f),
                SkillType.JUMPING to SkillEffectEntry(minHeight = 1.0f, maxHeight = 1.5f),
            )
    }
}

data class GeneralConfig(
    var maxLevel: Int = 100,
    var showLevelUpNotifications: Boolean = true,
    var showXpGainNotifications: Boolean = false,
)

data class XpConfig(
    var baseXpPerAction: Double = 1.0,
    var xpScaleFactor: Double = 1.0,
    var restedBonusMultiplier: Double = 1.5,
    var globalXpMultiplier: Double = 1.0,
    var actionXp: ActionXpConfig = ActionXpConfig(),
)

data class ActionXpConfig(
    var combatDamageMultiplier: Double = 0.1,
    var miningPerBlockMultiplier: Double = 1.0,
    var woodcuttingPerBlockMultiplier: Double = 1.0,
    var runningPerDistanceMultiplier: Double = 0.1,
    var swimmingPerDistanceMultiplier: Double = 0.1,
    var sneakingPerSecondMultiplier: Double = 0.1,
    var jumpingPerJumpMultiplier: Double = 0.5,
    var blockingDamageMultiplier: Double = 0.05,
    var divingPerSecondMultiplier: Double = 0.1,
)

data class DeathPenaltyConfig(
    var enabled: Boolean = true,
    var penaltyPercentage: Double = 0.1,
    var immunityDurationSeconds: Int = 300,
    var showImmunityInHud: Boolean = true,
)

data class SkillEffectEntry(
    var minDamage: Float? = null,
    var maxDamage: Float? = null,
    var minSpeed: Float? = null,
    var maxSpeed: Float? = null,
    var minSpeedBonus: Float? = null,
    var maxSpeedBonus: Float? = null,
    var minStaminaReduction: Float? = null,
    var maxStaminaReduction: Float? = null,
    var minHeight: Float? = null,
    var maxHeight: Float? = null,
    var minOxygenBonus: Float? = null,
    var maxOxygenBonus: Float? = null,
)
