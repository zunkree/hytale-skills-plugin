package org.zunkree.hytale.plugins.skillsplugin.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import kotlin.test.assertTrue

class ConfigValidationTest {
    @Test
    fun `default config is valid`() {
        assertDoesNotThrow { SkillsConfigValidator.validate(SkillsConfig()) }
    }

    @Test
    fun `rejects zero maxLevel`() {
        val config = SkillsConfig(general = GeneralConfig(maxLevel = 0))
        val ex = assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
        assertTrue(ex.message!!.contains("maxLevel"))
    }

    @Test
    fun `rejects negative maxLevel`() {
        val config = SkillsConfig(general = GeneralConfig(maxLevel = -1))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects zero baseXpPerAction`() {
        val config = SkillsConfig(xp = XpConfig(baseXpPerAction = 0.0))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative xpScaleFactor`() {
        val config = SkillsConfig(xp = XpConfig(xpScaleFactor = -1.0))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `accepts zero xpScaleFactor (flat curve)`() {
        val config = SkillsConfig(xp = XpConfig(xpScaleFactor = 0.0))
        assertDoesNotThrow { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects zero globalXpMultiplier`() {
        val config = SkillsConfig(xp = XpConfig(globalXpMultiplier = 0.0))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects restedBonusMultiplier below 1`() {
        val config = SkillsConfig(xp = XpConfig(restedBonusMultiplier = 0.5))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects penaltyPercentage above 1`() {
        val config = SkillsConfig(deathPenalty = DeathPenaltyConfig(penaltyPercentage = 1.5))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative penaltyPercentage`() {
        val config = SkillsConfig(deathPenalty = DeathPenaltyConfig(penaltyPercentage = -0.1))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative immunityDuration`() {
        val config = SkillsConfig(deathPenalty = DeathPenaltyConfig(immunityDurationSeconds = -1))
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `accepts boundary values`() {
        val config =
            SkillsConfig(
                general = GeneralConfig(maxLevel = 1),
                xp =
                    XpConfig(
                        baseXpPerAction = 0.001,
                        xpScaleFactor = 0.0,
                        globalXpMultiplier = 0.001,
                        restedBonusMultiplier = 1.0,
                    ),
                deathPenalty =
                    DeathPenaltyConfig(
                        penaltyPercentage = 0.0,
                        immunityDurationSeconds = 0,
                    ),
            )
        assertDoesNotThrow { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects inverted damage range`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.SWORDS to SkillEffectEntry(minDamage = 3.0f, maxDamage = 1.0f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative minDamage`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.SWORDS to SkillEffectEntry(minDamage = -1.0f, maxDamage = 2.0f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects stamina reduction above 1`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.BLOCKING to SkillEffectEntry(minStaminaReduction = 0.0f, maxStaminaReduction = 1.5f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects inverted speed range`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.MINING to SkillEffectEntry(minSpeed = 2.0f, maxSpeed = 1.0f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects inverted height range`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.JUMPING to SkillEffectEntry(minHeight = 2.0f, maxHeight = 1.0f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `accepts valid effect config`() {
        assertDoesNotThrow { SkillsConfigValidator.validate(SkillsConfig()) }
    }

    @Test
    fun `accepts effect entry with only minDamage set`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.SWORDS to SkillEffectEntry(minDamage = 1.0f),
                    ),
            )
        assertDoesNotThrow { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects inverted oxygen range`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.DIVING to SkillEffectEntry(minOxygenBonus = 1.0f, maxOxygenBonus = 0.0f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects inverted stamina reduction range`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.BLOCKING to SkillEffectEntry(minStaminaReduction = 0.5f, maxStaminaReduction = 0.1f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative minSpeed`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.MINING to SkillEffectEntry(minSpeed = -1.0f, maxSpeed = 1.5f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative minHeight`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.JUMPING to SkillEffectEntry(minHeight = -0.5f, maxHeight = 1.5f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects negative minStaminaReduction`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.BLOCKING to SkillEffectEntry(minStaminaReduction = -0.1f, maxStaminaReduction = 0.5f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `accepts empty skill effects map`() {
        val config = SkillsConfig(skillEffects = emptyMap())
        assertDoesNotThrow { SkillsConfigValidator.validate(config) }
    }

    @Test
    fun `rejects inverted speed bonus range`() {
        val config =
            SkillsConfig(
                skillEffects =
                    mapOf(
                        SkillType.RUNNING to SkillEffectEntry(minSpeedBonus = 0.5f, maxSpeedBonus = 0.1f),
                    ),
            )
        assertThrows<IllegalArgumentException> { SkillsConfigValidator.validate(config) }
    }
}
