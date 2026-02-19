package org.zunkree.hytale.plugins.skillsplugin.resolver

import org.junit.jupiter.api.Test
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovementXpPolicyTest {
    private val config = ActionXpConfig()
    private val policy = MovementXpPolicy(config)

    @Test
    fun `grants running XP when sprinting on land`() {
        val grants =
            policy.calculateGrants(
                isSprinting = true,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 10.0,
                deltaTime = 0.05,
            )
        assertEquals(1, grants.size)
        assertEquals(SkillType.RUNNING, grants[0].skillType)
        assertEquals(10.0 * config.runningPerDistanceMultiplier, grants[0].baseXp)
    }

    @Test
    fun `no running XP when running without sprinting`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 10.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.isEmpty())
    }

    @Test
    fun `grants swimming XP when sprinting in water`() {
        val grants =
            policy.calculateGrants(
                isSprinting = true,
                isSwimming = true,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 5.0,
                deltaTime = 0.05,
            )
        assertEquals(1, grants.size)
        assertEquals(SkillType.SWIMMING, grants[0].skillType)
    }

    @Test
    fun `no swimming XP when swimming without sprinting`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = true,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 5.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.isEmpty())
    }

    @Test
    fun `no running XP when sprinting in water`() {
        val grants =
            policy.calculateGrants(
                isSprinting = true,
                isSwimming = true,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 5.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.none { it.skillType == SkillType.RUNNING })
    }

    @Test
    fun `grants jumping XP on jump start only`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = true,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertEquals(1, grants.size)
        assertEquals(SkillType.JUMPING, grants[0].skillType)
    }

    @Test
    fun `no jumping XP when already jumping`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = true,
                wasJumping = true,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.none { it.skillType == SkillType.JUMPING })
    }

    @Test
    fun `grants diving XP when submerged`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = true,
                isCrouching = false,
                isIdle = false,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertEquals(1, grants.size)
        assertEquals(SkillType.DIVING, grants[0].skillType)
    }

    @Test
    fun `grants sneaking XP when crouching and not idle`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = true,
                isIdle = false,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertEquals(1, grants.size)
        assertEquals(SkillType.SNEAKING, grants[0].skillType)
    }

    @Test
    fun `no sneaking XP when crouching but idle`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = true,
                isIdle = true,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.isEmpty())
    }

    @Test
    fun `no XP when idle with no actions`() {
        val grants =
            policy.calculateGrants(
                isSprinting = false,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = true,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.isEmpty())
    }

    @Test
    fun `multiple grants per tick when sprinting and jumping`() {
        val grants =
            policy.calculateGrants(
                isSprinting = true,
                isSwimming = false,
                isJumping = true,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 5.0,
                deltaTime = 0.05,
            )
        assertEquals(2, grants.size)
        assertTrue(grants.any { it.skillType == SkillType.RUNNING })
        assertTrue(grants.any { it.skillType == SkillType.JUMPING })
    }

    @Test
    fun `no running XP with zero distance`() {
        val grants =
            policy.calculateGrants(
                isSprinting = true,
                isSwimming = false,
                isJumping = false,
                wasJumping = false,
                isDiving = false,
                isCrouching = false,
                isIdle = false,
                distance = 0.0,
                deltaTime = 0.05,
            )
        assertTrue(grants.isEmpty())
    }
}
