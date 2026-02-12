package org.zunkree.hytale.plugins.skillsplugin

object SkillsXpCalculator {

    private val config get() = SkillsPlugin.instance.config.xp

    fun xpRequiredForLevel(level: Int): Double =
        config.baseXpPerAction * 100.0 * (1 + (level - 1) * config.xpScaleFactor)

    fun cumulativeXpForLevel(level: Int): Double =
        (1..level).fold(0.0) { acc, i -> acc + xpRequiredForLevel(i) }

    fun calculateXpGain(baseXp: Double, isRested: Boolean): Double {
        val multiplier = if (isRested) config.restedBonusMultiplier else 1.0
        return baseXp * multiplier * config.globalXpMultiplier
    }
}
