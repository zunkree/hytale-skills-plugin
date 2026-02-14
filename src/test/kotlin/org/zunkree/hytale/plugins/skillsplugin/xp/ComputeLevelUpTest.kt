package org.zunkree.hytale.plugins.skillsplugin.xp

import org.junit.jupiter.api.Test
import org.zunkree.hytale.plugins.skillsplugin.config.XpConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ComputeLevelUpTest {
    private val xpCurve = XpCurve(XpConfig())

    @Test
    fun `returns null when already at max level`() {
        val result = computeLevelUp(100, 999999.0, 100.0, 100, xpCurve)
        assertNull(result)
    }

    @Test
    fun `returns null when xp gain is not enough for level up`() {
        val result = computeLevelUp(0, 0.0, 1.0, 100, xpCurve)
        assertNull(result)
    }

    @Test
    fun `returns level up when xp crosses threshold`() {
        val xpNeeded = xpCurve.cumulativeXpForLevel(1)
        val result = computeLevelUp(0, 0.0, xpNeeded, 100, xpCurve)
        assertNotNull(result)
        assertEquals(0, result.oldLevel)
        assertEquals(1, result.newLevel)
    }

    @Test
    fun `supports multi-level jump`() {
        val xpForLevel5 = xpCurve.cumulativeXpForLevel(5)
        val result = computeLevelUp(0, 0.0, xpForLevel5, 100, xpCurve)
        assertNotNull(result)
        assertEquals(0, result.oldLevel)
        assertEquals(5, result.newLevel)
    }

    @Test
    fun `caps at max level`() {
        val result = computeLevelUp(99, xpCurve.cumulativeXpForLevel(99), 999999.0, 100, xpCurve)
        assertNotNull(result)
        assertEquals(100, result.newLevel)
    }
}
