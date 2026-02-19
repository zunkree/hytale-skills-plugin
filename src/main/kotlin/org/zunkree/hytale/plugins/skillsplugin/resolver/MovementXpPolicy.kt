package org.zunkree.hytale.plugins.skillsplugin.resolver

import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

data class MovementXpGrant(
    val skillType: SkillType,
    val baseXp: Double,
)

class MovementXpPolicy(
    private val actionXpConfig: ActionXpConfig,
) {
    fun calculateGrants(
        isSprinting: Boolean,
        isSwimming: Boolean,
        isJumping: Boolean,
        wasJumping: Boolean,
        isDiving: Boolean,
        isCrouching: Boolean,
        isIdle: Boolean,
        distance: Double,
        deltaTime: Double,
    ): List<MovementXpGrant> {
        val grants = mutableListOf<MovementXpGrant>()

        if (isSprinting && !isSwimming && distance > 0) {
            grants += MovementXpGrant(SkillType.RUNNING, distance * actionXpConfig.runningPerDistanceMultiplier)
        }

        if (isSwimming && isSprinting && distance > 0) {
            grants += MovementXpGrant(SkillType.SWIMMING, distance * actionXpConfig.swimmingPerDistanceMultiplier)
        }

        if (isJumping && !wasJumping) {
            grants += MovementXpGrant(SkillType.JUMPING, actionXpConfig.jumpingPerJumpMultiplier)
        }

        if (isDiving) {
            grants += MovementXpGrant(SkillType.DIVING, actionXpConfig.divingPerSecondMultiplier * deltaTime)
        }

        if (isCrouching && !isIdle) {
            grants += MovementXpGrant(SkillType.SNEAKING, actionXpConfig.sneakingPerSecondMultiplier * deltaTime)
        }

        return grants
    }
}
