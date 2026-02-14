package org.zunkree.hytale.plugins.skillsplugin.xp

import org.junit.jupiter.api.Test
import org.zunkree.hytale.plugins.skillsplugin.config.XpConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XpCurveTest {
    private val defaultConfig = XpConfig()
    private val xpCurve = XpCurve(defaultConfig)

    @Test
    fun `xpRequiredForLevel returns base xp for level 1`() {
        val xp = xpCurve.xpRequiredForLevel(1)
        assertEquals(defaultConfig.baseXpPerAction * 100.0, xp)
    }

    @Test
    fun `xpRequiredForLevel scales with level`() {
        val level1 = xpCurve.xpRequiredForLevel(1)
        val level2 = xpCurve.xpRequiredForLevel(2)
        assertTrue(level2 > level1, "Higher levels should require more XP")
    }

    @Test
    fun `cumulativeXpForLevel at level 0 is 0`() {
        assertEquals(0.0, xpCurve.cumulativeXpForLevel(0))
    }

    @Test
    fun `cumulativeXpForLevel at level 1 equals xpRequiredForLevel 1`() {
        assertEquals(xpCurve.xpRequiredForLevel(1), xpCurve.cumulativeXpForLevel(1))
    }

    @Test
    fun `cumulativeXpForLevel closed-form matches iterative sum`() {
        for (level in 1..100) {
            val iterative = (1..level).sumOf { xpCurve.xpRequiredForLevel(it) }
            val closedForm = xpCurve.cumulativeXpForLevel(level)
            assertEquals(iterative, closedForm, 0.001, "Mismatch at level $level")
        }
    }

    @Test
    fun `calculateXpGain applies global multiplier`() {
        val config = XpConfig(globalXpMultiplier = 2.0)
        val curve = XpCurve(config)
        assertEquals(20.0, curve.calculateXpGain(10.0, isRested = false))
    }

    @Test
    fun `calculateXpGain applies rested bonus`() {
        val config = XpConfig(restedBonusMultiplier = 1.5, globalXpMultiplier = 1.0)
        val curve = XpCurve(config)
        assertEquals(15.0, curve.calculateXpGain(10.0, isRested = true))
    }

    @Test
    fun `calculateXpGain without rested uses multiplier of 1`() {
        val config = XpConfig(restedBonusMultiplier = 2.0, globalXpMultiplier = 1.0)
        val curve = XpCurve(config)
        assertEquals(10.0, curve.calculateXpGain(10.0, isRested = false))
    }

    @Test
    fun `levelProgress returns 0 for fresh skill`() {
        assertEquals(0.0, xpCurve.levelProgress(level = 0, totalXP = 0.0, maxLevel = 100))
    }

    @Test
    fun `levelProgress returns 1 at max level`() {
        assertEquals(1.0, xpCurve.levelProgress(level = 100, totalXP = 999999.0, maxLevel = 100))
    }

    @Test
    fun `levelProgress returns value between 0 and 1 for partial progress`() {
        val xpForLevel1 = xpCurve.cumulativeXpForLevel(1)
        val progress = xpCurve.levelProgress(level = 1, totalXP = xpForLevel1 + 1.0, maxLevel = 100)
        assertTrue(progress > 0.0, "Progress should be > 0")
        assertTrue(progress < 1.0, "Progress should be < 1")
    }
}
