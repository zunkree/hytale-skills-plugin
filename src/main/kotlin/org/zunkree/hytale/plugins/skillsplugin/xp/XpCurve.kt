package org.zunkree.hytale.plugins.skillsplugin.xp

import org.zunkree.hytale.plugins.skillsplugin.config.XpConfig

class XpCurve(
    private val xpConfig: XpConfig,
) {
    fun xpRequiredForLevel(level: Int): Double =
        xpConfig.baseXpPerAction * 100.0 * (1 + (level - 1) * xpConfig.xpScaleFactor)

    fun cumulativeXpForLevel(level: Int): Double =
        xpConfig.baseXpPerAction * 100.0 * (level + xpConfig.xpScaleFactor * level * (level - 1) / 2.0)

    fun calculateXpGain(
        baseXp: Double,
        isRested: Boolean,
    ): Double {
        val multiplier = if (isRested) xpConfig.restedBonusMultiplier else 1.0
        return baseXp * multiplier * xpConfig.globalXpMultiplier
    }

    fun levelProgress(
        level: Int,
        totalXP: Double,
        maxLevel: Int,
    ): Double {
        if (level >= maxLevel) return 1.0
        val xpForNext = cumulativeXpForLevel(level + 1)
        val xpForCurrent = cumulativeXpForLevel(level)
        val xpIntoLevel = totalXP - xpForCurrent
        val xpNeeded = xpForNext - xpForCurrent
        return (xpIntoLevel / xpNeeded).coerceIn(0.0, 1.0)
    }
}
