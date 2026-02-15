package org.zunkree.hytale.plugins.skillsplugin.xp

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.config.GeneralConfig
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

data class LevelUpResult(
    val skillType: SkillType,
    val oldLevel: Int,
    val newLevel: Int,
)

internal data class XpGrantResult(
    val newLevel: Int,
    val newTotalXP: Double,
    val levelUp: LevelUpResult?,
)

internal fun computeXpGrant(
    currentLevel: Int,
    currentTotalXP: Double,
    skillType: SkillType,
    baseXp: Double,
    maxLevel: Int,
    xpCurve: XpCurve,
    isRested: Boolean = false,
): XpGrantResult? {
    if (currentLevel >= maxLevel) return null

    val xpGain = xpCurve.calculateXpGain(baseXp, isRested)
    val newTotalXP = currentTotalXP + xpGain

    var newLevel = currentLevel

    while (newLevel < maxLevel) {
        val xpForNext = xpCurve.cumulativeXpForLevel(newLevel + 1)
        if (newTotalXP >= xpForNext) {
            newLevel++
        } else {
            break
        }
    }

    val levelUp =
        if (newLevel > currentLevel) {
            LevelUpResult(skillType, currentLevel, newLevel)
        } else {
            null
        }

    return XpGrantResult(newLevel, newTotalXP, levelUp)
}

class XpService(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val generalConfig: GeneralConfig,
    private val logger: HytaleLogger,
) {
    fun grantXp(
        playerRef: Ref<EntityStore>,
        skillType: SkillType,
        baseXp: Double,
        isRested: Boolean = false,
    ): LevelUpResult? {
        val skills =
            skillRepository.getPlayerSkills(playerRef)
                ?: run {
                    logger.info { "Failed to load player skills from ${playerRef.store}" }
                    return null
                }
        val skillData = skills.getSkill(skillType)

        if (skillData.level >= generalConfig.maxLevel) return null

        val result =
            computeXpGrant(
                skillData.level,
                skillData.totalXP,
                skillType,
                baseXp,
                generalConfig.maxLevel,
                xpCurve,
                isRested,
            ) ?: return null

        skillData.level = result.newLevel
        skillData.totalXP = result.newTotalXP
        return result.levelUp
    }

    fun grantXpAndSave(
        playerRef: Ref<EntityStore>,
        skillType: SkillType,
        baseXp: Double,
        commandBuffer: CommandBuffer<EntityStore>,
        isRested: Boolean = false,
    ) {
        val levelUp = grantXp(playerRef, skillType, baseXp, isRested)
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return
        skillRepository.savePlayerSkills(playerRef, skills, commandBuffer)
        if (levelUp != null) {
            notifyLevelUp(playerRef, levelUp)
        }
    }

    fun notifyLevelUp(
        playerRef: Ref<EntityStore>,
        result: LevelUpResult,
    ) {
        if (!generalConfig.showLevelUpNotifications) return

        val player =
            playerRef.store.getComponent(playerRef, componentType<Player>())
                ?: run {
                    logger.info { "Failed to load player entity from ${playerRef.store}" }
                    return
                }
        player.sendMessage(Message.raw("§a${result.skillType.displayName} increased to level ${result.newLevel}!"))
    }
}
