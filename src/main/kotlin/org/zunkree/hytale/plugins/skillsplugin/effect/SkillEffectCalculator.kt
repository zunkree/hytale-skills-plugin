package org.zunkree.hytale.plugins.skillsplugin.effect

import org.zunkree.hytale.plugins.skillsplugin.config.SkillEffectEntry
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

class SkillEffectCalculator(
    private val skillEffectsConfig: Map<SkillType, SkillEffectEntry>,
    private val maxLevel: Int,
) {
    init {
        require(maxLevel > 0) { "maxLevel must be > 0, got $maxLevel" }
    }

    fun getDamageMultiplier(
        skillType: SkillType,
        level: Int,
    ): Double {
        val entry = skillEffectsConfig[skillType] ?: return 1.0
        val min = entry.minDamage?.toDouble() ?: 1.0
        val max = entry.maxDamage?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getGatheringSpeedMultiplier(
        skillType: SkillType,
        level: Int,
    ): Double {
        val entry = skillEffectsConfig[skillType] ?: return 1.0
        val min = entry.minSpeed?.toDouble() ?: 1.0
        val max = entry.maxSpeed?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getSpeedBonus(
        skillType: SkillType,
        level: Int,
    ): Double {
        val entry = skillEffectsConfig[skillType] ?: return 0.0
        val min = entry.minSpeedBonus?.toDouble() ?: 0.0
        val max = entry.maxSpeedBonus?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    fun getStaminaReduction(
        skillType: SkillType,
        level: Int,
    ): Double {
        val entry = skillEffectsConfig[skillType] ?: return 0.0
        val min = entry.minStaminaReduction?.toDouble() ?: 0.0
        val max = entry.maxStaminaReduction?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    fun getJumpHeightMultiplier(level: Int): Double {
        val entry = skillEffectsConfig[SkillType.JUMPING] ?: return 1.0
        val min = entry.minHeight?.toDouble() ?: 1.0
        val max = entry.maxHeight?.toDouble() ?: 1.0
        return lerp(min, max, level)
    }

    fun getDivingOxygenBonus(level: Int): Double {
        val entry = skillEffectsConfig[SkillType.DIVING] ?: return 0.0
        val min = entry.minOxygenBonus?.toDouble() ?: 0.0
        val max = entry.maxOxygenBonus?.toDouble() ?: 0.0
        return lerp(min, max, level)
    }

    private fun lerp(
        min: Double,
        max: Double,
        level: Int,
    ): Double {
        val progress = (level.toDouble() / maxLevel).coerceIn(0.0, 1.0)
        return min + (max - min) * progress
    }
}
