package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

object SkillsManager {
    fun getPlayerSkills(playerRef: Ref<EntityStore>): SkillsComponent? {
        SkillsPlugin.instance.logger.debug { "Loading player skills from $playerRef" }
        return playerRef.store.getComponent(playerRef, componentType<SkillsComponent>())
    }

    fun savePlayerSkills(playerRef: Ref<EntityStore>, skills: SkillsComponent) {
        SkillsPlugin.instance.logger.info { "Saving player skills to $playerRef" }
        playerRef.store.putComponent(playerRef, componentType<SkillsComponent>(), skills)
    }

    fun savePlayerSkills(
        playerRef: Ref<EntityStore>,
        skills: SkillsComponent,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        SkillsPlugin.instance.logger.info { "Saving player skills via command buffer to $playerRef" }
        commandBuffer.putComponent(playerRef, componentType<SkillsComponent>(), skills)
    }

    fun getSkillLevel(playerRef: Ref<EntityStore>, skillType: SkillsType): Int {
        SkillsPlugin.instance.logger.info { "Retrieving skill level for ${skillType.name} from $playerRef" }
        val skills = getPlayerSkills(playerRef) ?: run {
            SkillsPlugin.instance.logger.info { "Failed to load player skills from $playerRef" }
            return 0
        }
        return skills.getSkill(skillType).level
    }

    fun getSkillData(playerRef: Ref<EntityStore>, skillType: SkillsType): SkillsData {
        SkillsPlugin.instance.logger.info { "Retrieving skill data for ${skillType.name} from $playerRef" }
        val skills = getPlayerSkills(playerRef)
            ?: run {
                SkillsPlugin.instance.logger.info { "Failed to load player skills from $playerRef" }
                return SkillsData()
            }
        return skills.getSkill(skillType)
    }
}
