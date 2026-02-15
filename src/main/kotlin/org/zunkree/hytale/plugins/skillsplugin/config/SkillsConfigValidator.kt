package org.zunkree.hytale.plugins.skillsplugin.config

object SkillsConfigValidator {
    fun validate(config: SkillsConfig) {
        with(config.general) {
            require(maxLevel > 0) { "maxLevel must be > 0, got $maxLevel" }
        }
        with(config.xp) {
            require(baseXpPerAction > 0) { "baseXpPerAction must be > 0, got $baseXpPerAction" }
            require(xpScaleFactor >= 0) { "xpScaleFactor must be >= 0, got $xpScaleFactor" }
            require(globalXpMultiplier > 0) { "globalXpMultiplier must be > 0, got $globalXpMultiplier" }
            require(restedBonusMultiplier >= 1.0) { "restedBonusMultiplier must be >= 1.0, got $restedBonusMultiplier" }
        }
        with(config.deathPenalty) {
            require(penaltyPercentage in 0.0..1.0) {
                "penaltyPercentage must be in 0.0..1.0, got $penaltyPercentage"
            }
            require(immunityDurationSeconds >= 0) {
                "immunityDurationSeconds must be >= 0, got $immunityDurationSeconds"
            }
        }
        validateEffectConfig(config.skillEffects)
    }

    private fun validateEffectConfig(effects: Map<*, SkillEffectEntry>) {
        for ((skillType, entry) in effects) {
            val name = skillType.toString()
            entry.minDamage?.let { min ->
                entry.maxDamage?.let { max ->
                    require(min >= 0f) { "$name: minDamage must be >= 0, got $min" }
                    require(min <= max) { "$name: minDamage ($min) must be <= maxDamage ($max)" }
                }
            }
            entry.minSpeed?.let { min ->
                entry.maxSpeed?.let { max ->
                    require(min >= 0f) { "$name: minSpeed must be >= 0, got $min" }
                    require(min <= max) { "$name: minSpeed ($min) must be <= maxSpeed ($max)" }
                }
            }
            entry.minSpeedBonus?.let { min ->
                entry.maxSpeedBonus?.let { max ->
                    require(min <= max) { "$name: minSpeedBonus ($min) must be <= maxSpeedBonus ($max)" }
                }
            }
            entry.minStaminaReduction?.let { min ->
                entry.maxStaminaReduction?.let { max ->
                    require(min >= 0f) { "$name: minStaminaReduction must be >= 0, got $min" }
                    require(max <= 1f) { "$name: maxStaminaReduction must be <= 1.0, got $max" }
                    require(min <= max) { "$name: minStaminaReduction ($min) must be <= maxStaminaReduction ($max)" }
                }
            }
            entry.minHeight?.let { min ->
                entry.maxHeight?.let { max ->
                    require(min >= 0f) { "$name: minHeight must be >= 0, got $min" }
                    require(min <= max) { "$name: minHeight ($min) must be <= maxHeight ($max)" }
                }
            }
            entry.minOxygenBonus?.let { min ->
                entry.maxOxygenBonus?.let { max ->
                    require(min <= max) { "$name: minOxygenBonus ($min) must be <= maxOxygenBonus ($max)" }
                }
            }
        }
    }
}
