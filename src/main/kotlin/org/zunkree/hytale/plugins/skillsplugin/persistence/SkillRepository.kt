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
    fun getPlayerSkills(ref: Ref<EntityStore>): PlayerSkillsComponent? {
        logger.debug { "Loading player skills from $ref" }
        return ref.store.getComponent(ref, componentType<PlayerSkillsComponent>())
    }

    fun savePlayerSkills(
        ref: Ref<EntityStore>,
        skills: PlayerSkillsComponent,
    ) {
        logger.debug { "Saving player skills to $ref" }
        ref.store.putComponent(ref, componentType<PlayerSkillsComponent>(), skills)
    }

    fun savePlayerSkills(
        ref: Ref<EntityStore>,
        skills: PlayerSkillsComponent,
        commandBuffer: CommandBuffer<EntityStore>,
    ) {
        logger.debug { "Saving player skills via command buffer to $ref" }
        commandBuffer.putComponent(ref, componentType<PlayerSkillsComponent>(), skills)
    }

    fun getSkillLevel(
        ref: Ref<EntityStore>,
        skillType: SkillType,
    ): Int? {
        logger.debug { "Retrieving skill level for ${skillType.name} from $ref" }
        val skills =
            getPlayerSkills(ref) ?: run {
                logger.debug { "No player skills found for $ref" }
                return null
            }
        return skills.getSkill(skillType).level
    }

    fun getSkillData(
        ref: Ref<EntityStore>,
        skillType: SkillType,
    ): SkillData? {
        logger.debug { "Retrieving skill data for ${skillType.name} from $ref" }
        val skills =
            getPlayerSkills(ref)
                ?: run {
                    logger.debug { "No player skills found for $ref" }
                    return null
                }
        return skills.getSkill(skillType)
    }
}
