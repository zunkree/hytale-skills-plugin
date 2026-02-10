package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

data class LevelUpResult(
    val skillType: SkillsType,
    val oldLevel: Int,
    val newLevel: Int,
)

object SkillsXpService {

    fun grantXp(
        playerRef: Ref<EntityStore>,
        skillType: SkillsType,
        baseXp: Float,
        isRested: Boolean = false
    ): LevelUpResult? {
        val skills = SkillsManager.getPlayerSkills(playerRef)
            ?: run {
                SkillsPlugin.instance.logger.info { "Failed to load player skills from ${playerRef.store}" }
                return null
            }
        val skillData = skills.getSkill(skillType)

        if (skillData.level >= SkillsData.MAX_LEVEL) {
            return null // Already at max level
        }

        val xpGain = SkillsXpCalculator.calculateXpGain(baseXp, isRested)
        skillData.totalXP += xpGain

        val oldLevel = skillData.level
        var newLevel = oldLevel

        while (newLevel < SkillsData.MAX_LEVEL) {
            val xpForNext = SkillsXpCalculator.cumulativeXpForLevel(newLevel + 1)
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

    fun notifyLevelUp(playerRef: Ref<EntityStore>, result: LevelUpResult) {
        val player = playerRef.store.getComponent(playerRef, componentType<Player>())
            ?: run {
                SkillsPlugin.instance.logger.info { "Failed to load player entity from ${playerRef.store}" }
                return
            }
        player.sendMessage(Message.raw("§a${result.skillType.displayName} increased to level ${result.newLevel}!"))
    }
}
