package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import org.zunkree.hytale.plugins.skillsplugin.SkillsComponent
import org.zunkree.hytale.plugins.skillsplugin.SkillsManager
import org.zunkree.hytale.plugins.skillsplugin.SkillsManager.getPlayerSkills
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin

class SkillsPlayerEventListener {
    private val logger get() = SkillsPlugin.instance.logger
    private val activePlayers get() = SkillsPlugin.instance.activePlayers

    fun onPlayerReady(event: PlayerReadyEvent) {
        val playerRef = event.playerRef

        logger.info { "Fetching skills for ${event.player.displayName}:${event.player.uuid}" }
        if (getPlayerSkills(playerRef) == null) {
            logger.info { "No skills found for ${event.player.displayName}:${event.player.uuid}, initializing new skills component." }
            SkillsManager.savePlayerSkills(playerRef, SkillsComponent())
            logger.info { "Initialized skills for ${event.player.displayName}:${event.player.uuid}" }
        }

        val uuid = event.player.uuid ?: return
        SkillsPlugin.instance.activePlayers[uuid] = playerRef
    }

    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        logger.info { "Player ${event.playerRef.username} is disconnecting, saving skills and cleaning up." }

        val entityRef = activePlayers.remove(event.playerRef.uuid) ?: return
        val skills = getPlayerSkills(entityRef) ?: return
        SkillsManager.savePlayerSkills(entityRef, skills)
        logger.info { "Saved skills for player ${event.playerRef.username} (${event.playerRef.uuid}) on disconnect." }
    }
}
