package org.zunkree.hytale.plugins.skillsplugin

object SkillsXpCalculator {

    private val config get() = SkillsPlugin.instance.config.xp

    fun xpRequiredForLevel(level: Int): Float {
        return config.baseXpPerAction * 100f * (1 + (level - 1) * config.xpScaleFactor)
    }

    fun cumulativeXpForLevel(level: Int): Float {
        var total = 0f
        for (i in 1..level) {
            total += xpRequiredForLevel(i)
        }
        return total
    }

    fun calculateXpGain(baseXp: Float, isRested: Boolean): Float {
        val restedBonus = config.restedBonusMultiplier - 1f
        val multiplier = if (isRested) 1f + restedBonus else 1f
        return baseXp * multiplier * config.globalXpMultiplier
    }
}
