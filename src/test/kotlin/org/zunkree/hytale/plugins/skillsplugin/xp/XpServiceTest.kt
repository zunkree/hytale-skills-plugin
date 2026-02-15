package org.zunkree.hytale.plugins.skillsplugin.xp

import org.junit.jupiter.api.Test
import org.zunkree.hytale.plugins.skillsplugin.config.XpConfig
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class XpServiceTest {
    private val xpConfig = XpConfig()
    private val xpCurve = XpCurve(xpConfig)
    private val maxLevel = 100

    @Test
    fun `xp accumulates without level-up`() {
        val result = computeXpGrant(0, 0.0, SkillType.SWORDS, 1.0, maxLevel, xpCurve)
        assertNotNull(result)
        assertNull(result.levelUp)
        assertEquals(1.0, result.newTotalXP)
    }

    @Test
    fun `multi-grant accumulation across calls`() {
        val first = computeXpGrant(0, 0.0, SkillType.SWORDS, 5.0, maxLevel, xpCurve)!!
        val second =
            computeXpGrant(
                first.newLevel,
                first.newTotalXP,
                SkillType.SWORDS,
                3.0,
                maxLevel,
                xpCurve,
            )!!
        assertEquals(8.0, second.newTotalXP)
    }

    @Test
    fun `returns null at max level`() {
        val result = computeXpGrant(maxLevel, 999999.0, SkillType.SWORDS, 100.0, maxLevel, xpCurve)
        assertNull(result)
    }

    @Test
    fun `returns LevelUpResult on level-up`() {
        val xpForLevel1 = xpCurve.cumulativeXpForLevel(1)
        val result = computeXpGrant(0, 0.0, SkillType.MINING, xpForLevel1, maxLevel, xpCurve)
        assertNotNull(result)
        assertNotNull(result.levelUp)
        assertEquals(SkillType.MINING, result.levelUp.skillType)
        assertEquals(0, result.levelUp.oldLevel)
        assertEquals(1, result.levelUp.newLevel)
        assertEquals(1, result.newLevel)
    }

    @Test
    fun `multi-level jump from large xp grant`() {
        val xpForLevel5 = xpCurve.cumulativeXpForLevel(5)
        val result = computeXpGrant(0, 0.0, SkillType.RUNNING, xpForLevel5, maxLevel, xpCurve)
        assertNotNull(result)
        assertNotNull(result.levelUp)
        assertEquals(0, result.levelUp.oldLevel)
        assertEquals(5, result.levelUp.newLevel)
        assertEquals(5, result.newLevel)
    }

    @Test
    fun `level capped at maxLevel`() {
        val result =
            computeXpGrant(
                99,
                xpCurve.cumulativeXpForLevel(99),
                SkillType.SWORDS,
                999999.0,
                maxLevel,
                xpCurve,
            )
        assertNotNull(result)
        assertNotNull(result.levelUp)
        assertEquals(maxLevel, result.newLevel)
    }

    @Test
    fun `rested bonus increases xp gain`() {
        val result =
            computeXpGrant(
                0,
                0.0,
                SkillType.SWORDS,
                10.0,
                maxLevel,
                xpCurve,
                isRested = true,
            )
        assertNotNull(result)
        val expectedXp = 10.0 * xpConfig.restedBonusMultiplier
        assertEquals(expectedXp, result.newTotalXP)
    }

    @Test
    fun `xp is correctly accumulated in result`() {
        val xpForLevel3 = xpCurve.cumulativeXpForLevel(3)
        val result = computeXpGrant(0, 0.0, SkillType.SWORDS, xpForLevel3, maxLevel, xpCurve)
        assertNotNull(result)
        assertEquals(3, result.newLevel)
        assertEquals(xpForLevel3, result.newTotalXP)
    }
}
