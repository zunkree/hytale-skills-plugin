package org.zunkree.hytale.plugins.skillsplugin.persistence

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.skill.PlayerSkillsComponent
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillData
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

class SkillRepository(
    private val logger: HytaleLogger,
) {
    fun getPlayerSkills(playerRef: Ref<EntityStore>): PlayerSkillsComponent? {
        logger.debug { "Loading player skills from $playerRef" }
        return playerRef.store.getComponent(playerRef, componentType<PlayerSkillsComponent>())
    }

    fun savePlayerSkills(
        playerRef: Ref<EntityStore>,
        skills: PlayerSkillsComponent,
    ) {
        logger.debug { "Saving player skills to $playerRef" }
        playerRef.store.putComponent(playerRef, componentType<PlayerSkillsComponent>(), skills)
    }

    fun savePlayerSkills(
        playerRef: Ref<EntityStore>,
        skills: PlayerSkillsComponent,
        commandBuffer: CommandBuffer<EntityStore>,
    ) {
        logger.debug { "Saving player skills via command buffer to $playerRef" }
        commandBuffer.putComponent(playerRef, componentType<PlayerSkillsComponent>(), skills)
    }

    fun getSkillLevel(
        playerRef: Ref<EntityStore>,
        skillType: SkillType,
    ): Int {
        logger.debug { "Retrieving skill level for ${skillType.name} from $playerRef" }
        val skills =
            getPlayerSkills(playerRef) ?: run {
                logger.debug { "Failed to load player skills from $playerRef" }
                return 0
            }
        return skills.getSkill(skillType).level
    }

    fun getSkillData(
        playerRef: Ref<EntityStore>,
        skillType: SkillType,
    ): SkillData {
        logger.debug { "Retrieving skill data for ${skillType.name} from $playerRef" }
        val skills =
            getPlayerSkills(playerRef)
                ?: run {
                    logger.debug { "Failed to load player skills from $playerRef" }
                    return SkillData()
                }
        return skills.getSkill(skillType)
    }
}
