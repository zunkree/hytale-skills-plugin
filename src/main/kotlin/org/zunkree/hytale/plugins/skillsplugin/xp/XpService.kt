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

fun computeLevelUp(
    currentLevel: Int,
    currentXp: Double,
    xpGain: Double,
    maxLevel: Int,
    xpCurve: XpCurve,
): LevelUpResult? {
    if (currentLevel >= maxLevel) return null
    val newTotalXp = currentXp + xpGain
    var newLevel = currentLevel
    while (newLevel < maxLevel) {
        if (newTotalXp >= xpCurve.cumulativeXpForLevel(newLevel + 1)) {
            newLevel++
        } else {
            break
        }
    }
    return if (newLevel > currentLevel) {
        LevelUpResult(SkillType.SWORDS, currentLevel, newLevel) // skillType set by caller
    } else {
        null
    }
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

        if (skillData.level >= generalConfig.maxLevel) {
            return null
        }

        val xpGain = xpCurve.calculateXpGain(baseXp, isRested)
        skillData.totalXP += xpGain

        val oldLevel = skillData.level
        var newLevel = oldLevel

        while (newLevel < generalConfig.maxLevel) {
            val xpForNext = xpCurve.cumulativeXpForLevel(newLevel + 1)
            if (skillData.totalXP >= xpForNext) {
                newLevel++
            } else {
                break
            }
        }

        skillData.level = newLevel

        return if (newLevel > oldLevel) {
            LevelUpResult(skillType, oldLevel, newLevel)
        } else {
            null
        }
    }

    fun grantXpAndSave(
        playerRef: Ref<EntityStore>,
        skillType: SkillType,
        baseXp: Double,
        commandBuffer: CommandBuffer<EntityStore>,
        isRested: Boolean = false,
    ) {
        val result = grantXp(playerRef, skillType, baseXp, isRested) ?: return
        val skills = skillRepository.getPlayerSkills(playerRef) ?: return
        skillRepository.savePlayerSkills(playerRef, skills, commandBuffer)
        notifyLevelUp(playerRef, result)
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
