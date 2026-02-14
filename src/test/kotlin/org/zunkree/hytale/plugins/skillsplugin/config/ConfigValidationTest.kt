package org.zunkree.hytale.plugins.skillsplugin.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class ConfigValidationTest {
    private fun validate(config: SkillsConfig) {
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
            require(penaltyPercentage in 0.0..1.0) { "penaltyPercentage must be in 0.0..1.0, got $penaltyPercentage" }
            require(immunityDurationSeconds >= 0) {
                "immunityDurationSeconds must be >= 0, got $immunityDurationSeconds"
            }
        }
    }

    @Test
    fun `default config is valid`() {
        assertDoesNotThrow { validate(SkillsConfig()) }
    }

    @Test
    fun `rejects zero maxLevel`() {
        val config = SkillsConfig(general = GeneralConfig(maxLevel = 0))
        val ex = assertThrows<IllegalArgumentException> { validate(config) }
        assertTrue(ex.message!!.contains("maxLevel"))
    }

    @Test
    fun `rejects negative maxLevel`() {
        val config = SkillsConfig(general = GeneralConfig(maxLevel = -1))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `rejects zero baseXpPerAction`() {
        val config = SkillsConfig(xp = XpConfig(baseXpPerAction = 0.0))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `rejects negative xpScaleFactor`() {
        val config = SkillsConfig(xp = XpConfig(xpScaleFactor = -1.0))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `accepts zero xpScaleFactor (flat curve)`() {
        val config = SkillsConfig(xp = XpConfig(xpScaleFactor = 0.0))
        assertDoesNotThrow { validate(config) }
    }

    @Test
    fun `rejects zero globalXpMultiplier`() {
        val config = SkillsConfig(xp = XpConfig(globalXpMultiplier = 0.0))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `rejects restedBonusMultiplier below 1`() {
        val config = SkillsConfig(xp = XpConfig(restedBonusMultiplier = 0.5))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `rejects penaltyPercentage above 1`() {
        val config = SkillsConfig(deathPenalty = DeathPenaltyConfig(penaltyPercentage = 1.5))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `rejects negative penaltyPercentage`() {
        val config = SkillsConfig(deathPenalty = DeathPenaltyConfig(penaltyPercentage = -0.1))
        assertThrows<IllegalArgumentException> { validate(config) }
    }

    @Test
    fun `rejects negative immunityDuration`() {
        val config = SkillsConfig(deathPenalty = DeathPenaltyConfig(immunityDurationSeconds = -1))
        assertThrows<IllegalArgumentException> { validate(config) }
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
        assertDoesNotThrow { validate(config) }
    }
}
