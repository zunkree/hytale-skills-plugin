package org.zunkree.hytale.plugins.skillsplugin

import kotlinx.serialization.Serializable

@Serializable
data class SkillsConfig(
    val general: GeneralConfig = GeneralConfig(),
    val xp: XpConfig = XpConfig(),
    val deathPenalty: DeathPenaltyConfig = DeathPenaltyConfig(),
    val skillEffects: Map<String, SkillEffectEntry> = defaultSkillEffects(),
) {
    companion object {
        fun defaultSkillEffects(): Map<String, SkillEffectEntry> = mapOf(
            "SWORDS" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "AXES" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "DAGGERS" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "BOWS" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "SPEARS" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "CLUBS" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "UNARMED" to SkillEffectEntry(minDamage = 1.0f, maxDamage = 2.0f),
            "BLOCKING" to SkillEffectEntry(minBlockPower = 1.0f, maxBlockPower = 1.5f),
            "MINING" to SkillEffectEntry(minSpeed = 1.0f, maxSpeed = 1.5f),
            "WOODCUTTING" to SkillEffectEntry(minSpeed = 1.0f, maxSpeed = 1.5f),
            "RUNNING" to SkillEffectEntry(
                minSpeedBonus = 0.0f, maxSpeedBonus = 0.25f,
                minStaminaReduction = 0.0f, maxStaminaReduction = 0.33f
            ),
            "SWIMMING" to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.5f),
            "DIVING" to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.5f),
            "SNEAKING" to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 0.75f),
            "JUMPING" to SkillEffectEntry(minHeight = 1.0f, maxHeight = 1.5f)
        )
    }
}

@Serializable
data class GeneralConfig(
    val maxLevel: Int = 100,
    val showLevelUpNotifications: Boolean = true,
    val showXpGainNotifications: Boolean = false,
    var debugLogging: Boolean = false,
)

@Serializable
data class XpConfig(
    val baseXpPerAction: Double = 1.0,
    val xpScaleFactor: Double = 1.0,
    var restedBonusMultiplier: Double = 1.5,
    val globalXpMultiplier: Double = 1.0,
    val actionXp: ActionXpConfig = ActionXpConfig(),
)

@Serializable
data class ActionXpConfig(
    val combatDamageMultiplier: Double = 0.1,
    val miningPerBlockMultiplier: Double = 1.0,
    val woodcuttingPerBlockMultiplier: Double = 1.0,
    val runningPerDistanceMultiplier: Double = 0.1,
    val swimmingPerDistanceMultiplier: Double = 0.1,
    val sneakingPerSecondMultiplier: Double = 0.1,
    val jumpingPerJumpMultiplier: Double = 0.5,
    val blockingDamageMultiplier: Double = 0.05,
    val divingPerSecondMultiplier: Double = 0.1,
)

@Serializable
data class DeathPenaltyConfig(
    val enabled: Boolean = true,
    val penaltyPercentage: Double = 0.1,
    val immunityDurationSeconds: Int = 300,
    val showImmunityInHud: Boolean = true,
)

@Serializable
data class SkillEffectEntry(
    val minDamage: Float? = null,
    val maxDamage: Float? = null,
    val minBlockPower: Float? = null,
    val maxBlockPower: Float? = null,
    val minSpeed: Float? = null,
    val maxSpeed: Float? = null,
    val minSpeedBonus: Float? = null,
    val maxSpeedBonus: Float? = null,
    val minStaminaReduction: Float? = null,
    val maxStaminaReduction: Float? = null,
    val minHeight: Float? = null,
    val maxHeight: Float? = null
)
