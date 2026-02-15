package org.zunkree.hytale.plugins.skillsplugin.effect

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.zunkree.hytale.plugins.skillsplugin.config.SkillEffectEntry
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfig
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkillEffectCalculatorTest {
    private val defaultEffects = SkillsConfig.defaultSkillEffects()
    private val maxLevel = 100
    private val calculator = SkillEffectCalculator(defaultEffects, maxLevel)

    @Test
    fun `getDamageMultiplier at level 0 returns min`() {
        assertEquals(1.0, calculator.getDamageMultiplier(SkillType.SWORDS, 0))
    }

    @Test
    fun `getDamageMultiplier at max level returns max`() {
        assertEquals(2.0, calculator.getDamageMultiplier(SkillType.SWORDS, 100))
    }

    @Test
    fun `getDamageMultiplier at midpoint returns midpoint value`() {
        assertEquals(1.5, calculator.getDamageMultiplier(SkillType.SWORDS, 50))
    }

    @Test
    fun `getDamageMultiplier for missing skill type returns 1`() {
        val calc = SkillEffectCalculator(emptyMap(), maxLevel)
        assertEquals(1.0, calc.getDamageMultiplier(SkillType.SWORDS, 50))
    }

    @Test
    fun `getGatheringSpeedMultiplier at level 0 returns min`() {
        assertEquals(1.0, calculator.getGatheringSpeedMultiplier(SkillType.MINING, 0))
    }

    @Test
    fun `getGatheringSpeedMultiplier at max level returns max`() {
        assertEquals(1.5, calculator.getGatheringSpeedMultiplier(SkillType.MINING, 100))
    }

    @Test
    fun `getGatheringSpeedMultiplier for missing skill returns 1`() {
        val calc = SkillEffectCalculator(emptyMap(), maxLevel)
        assertEquals(1.0, calc.getGatheringSpeedMultiplier(SkillType.MINING, 50))
    }

    @Test
    fun `getSpeedBonus at level 0 returns 0`() {
        assertEquals(0.0, calculator.getSpeedBonus(SkillType.RUNNING, 0))
    }

    @Test
    fun `getSpeedBonus at max level returns max`() {
        assertEquals(0.25, calculator.getSpeedBonus(SkillType.RUNNING, 100))
    }

    @Test
    fun `getSpeedBonus for missing skill returns 0`() {
        val calc = SkillEffectCalculator(emptyMap(), maxLevel)
        assertEquals(0.0, calc.getSpeedBonus(SkillType.RUNNING, 50))
    }

    @Test
    fun `getStaminaReduction at level 0 returns 0`() {
        assertEquals(0.0, calculator.getStaminaReduction(SkillType.BLOCKING, 0))
    }

    @Test
    fun `getStaminaReduction at max level returns max`() {
        assertEquals(0.5, calculator.getStaminaReduction(SkillType.BLOCKING, 100))
    }

    @Test
    fun `getStaminaReduction for running at max level`() {
        assertEquals(0.33, calculator.getStaminaReduction(SkillType.RUNNING, 100), 0.001)
    }

    @Test
    fun `getJumpHeightMultiplier at level 0 returns min`() {
        assertEquals(1.0, calculator.getJumpHeightMultiplier(0))
    }

    @Test
    fun `getJumpHeightMultiplier at max level returns max`() {
        assertEquals(1.5, calculator.getJumpHeightMultiplier(100))
    }

    @Test
    fun `getJumpHeightMultiplier at midpoint`() {
        assertEquals(1.25, calculator.getJumpHeightMultiplier(50))
    }

    @Test
    fun `getDivingOxygenBonus at level 0 returns 0`() {
        assertEquals(0.0, calculator.getDivingOxygenBonus(0))
    }

    @Test
    fun `getDivingOxygenBonus at max level returns max`() {
        assertEquals(1.0, calculator.getDivingOxygenBonus(100))
    }

    @Test
    fun `getDivingOxygenBonus for missing config returns 0`() {
        val calc = SkillEffectCalculator(emptyMap(), maxLevel)
        assertEquals(0.0, calc.getDivingOxygenBonus(50))
    }

    @Test
    fun `lerp interpolates linearly for all weapon types`() {
        val weaponTypes =
            listOf(
                SkillType.AXES,
                SkillType.BOWS,
                SkillType.SPEARS,
                SkillType.CLUBS,
                SkillType.UNARMED,
            )
        for (weaponType in weaponTypes) {
            assertEquals(1.0, calculator.getDamageMultiplier(weaponType, 0))
            assertEquals(2.0, calculator.getDamageMultiplier(weaponType, 100))
            assertEquals(1.5, calculator.getDamageMultiplier(weaponType, 50))
        }
    }

    @Test
    fun `lerp clamps negative level to min value`() {
        assertEquals(1.0, calculator.getDamageMultiplier(SkillType.SWORDS, -10))
    }

    @Test
    fun `lerp clamps level above max to max value`() {
        assertEquals(2.0, calculator.getDamageMultiplier(SkillType.SWORDS, 200))
    }

    @Test
    fun `getSpeedBonus clamps negative level to 0`() {
        assertEquals(0.0, calculator.getSpeedBonus(SkillType.RUNNING, -5))
    }

    @Test
    fun `getStaminaReduction clamps level above max`() {
        assertEquals(0.5, calculator.getStaminaReduction(SkillType.BLOCKING, 150))
    }

    @Test
    fun `constructor rejects maxLevel of zero`() {
        assertThrows<IllegalArgumentException> {
            SkillEffectCalculator(defaultEffects, 0)
        }
    }

    @Test
    fun `constructor rejects negative maxLevel`() {
        assertThrows<IllegalArgumentException> {
            SkillEffectCalculator(defaultEffects, -1)
        }
    }

    @Test
    fun `custom config values are respected`() {
        val customEffects =
            mapOf(
                SkillType.SWORDS to SkillEffectEntry(minDamage = 2.0f, maxDamage = 4.0f),
            )
        val calc = SkillEffectCalculator(customEffects, 100)
        assertEquals(2.0, calc.getDamageMultiplier(SkillType.SWORDS, 0))
        assertEquals(4.0, calc.getDamageMultiplier(SkillType.SWORDS, 100))
        assertEquals(3.0, calc.getDamageMultiplier(SkillType.SWORDS, 50))
    }

    private fun assertEquals(
        expected: Double,
        actual: Double,
        tolerance: Double,
    ) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "Expected $expected but was $actual (tolerance: $tolerance)",
        )
    }
}
