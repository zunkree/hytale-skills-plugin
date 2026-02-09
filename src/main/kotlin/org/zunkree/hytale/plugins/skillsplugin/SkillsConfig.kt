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
    val baseXpPerAction: Float = 1.0f,
    val xpScaleFactor: Float = 1.0f,
    var restedBonusMultiplier: Float = 1.5f,
    val globalXpMultiplier: Float = 1.0f,
    val actionXp: ActionXpConfig = ActionXpConfig(),
)

@Serializable
data class ActionXpConfig(
    val combatDamageMultiplier: Float = 0.1f,
    val miningPerBlockMultiplier: Float = 1.0f,
    val woodcuttingPerBlockMultiplier: Float = 1.0f,
    val runningPerSecondMultiplier: Float = 0.1f,
    val swimmingPerSecondMultiplier: Float = 0.1f,
    val sneakingPerSecondMultiplier: Float = 0.1f,
    val jumpingPerJumpMultiplier: Float = 0.5f,
    val blockingDamageMultiplier: Float = 0.05f,
)

@Serializable
data class DeathPenaltyConfig(
    val enabled: Boolean = true,
    val penaltyPercentage: Float = 0.1f,
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