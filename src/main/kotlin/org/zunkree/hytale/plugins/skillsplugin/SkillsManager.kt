package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

object SkillsManager {
    fun getPlayerSkills(playerRef: Ref<EntityStore>): SkillsComponent? {
        SkillsPlugin.instance.logger.info { "Loading player skills from $playerRef" }
        val player = getPlayerFromRef(playerRef)
            ?: run {
                SkillsPlugin.instance.logger.info { "Failed to load player from $playerRef" }
                return null
            }
        player.sendMessage(Message.raw("Loading skills..."))
        return playerRef.store.getComponent(playerRef, componentType<SkillsComponent>())
    }

    fun savePlayerSkills(playerRef: Ref<EntityStore>, skills: SkillsComponent) {
        SkillsPlugin.instance.logger.info { "Saving player skills to $playerRef" }
        val player = getPlayerFromRef(playerRef)
            ?: run {
                SkillsPlugin.instance.logger.info { "Failed to load player from $playerRef" }
                return
            }
        player.sendMessage(Message.raw("Saving skills..."))
        playerRef.store.putComponent(playerRef, componentType<SkillsComponent>(), skills)
    }

    fun getSkillLevel(playerRef: Ref<EntityStore>, skillType: SkillsType): Int {
        SkillsPlugin.instance.logger.info { "Retrieving skill level for ${skillType.name} from $playerRef" }
        val skills = getPlayerSkills(playerRef)
            ?: run {
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

    fun getPlayerFromRef(playerRef: Ref<EntityStore>): Player? {
        val player = playerRef.store.getComponent(playerRef, componentType<Player>())
        return player
    }
}