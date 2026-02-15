package org.zunkree.hytale.plugins.skillsplugin.listener

import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.skill.PlayerSkillsComponent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerLifecycleListener(
    private val skillRepository: SkillRepository,
    private val activePlayers: ConcurrentHashMap<UUID, Ref<EntityStore>>,
    private val logger: HytaleLogger,
) {
    fun onPlayerReady(event: PlayerReadyEvent) {
        val playerRef = event.playerRef

        logger.debug { "Fetching skills for ${event.player.displayName}:${event.player.uuid}" }
        if (skillRepository.getPlayerSkills(playerRef) == null) {
            logger.info {
                "No skills found for ${event.player.displayName}:${event.player.uuid}, initializing."
            }
            skillRepository.savePlayerSkills(playerRef, PlayerSkillsComponent())
            logger.debug { "Initialized skills for ${event.player.displayName}:${event.player.uuid}" }
        }

        val uuid = event.player.uuid ?: return
        activePlayers[uuid] = playerRef
    }

    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        logger.info { "Player ${event.playerRef.username} is disconnecting, saving skills and cleaning up." }

        val entityRef = activePlayers.remove(event.playerRef.uuid) ?: return
        val skills = skillRepository.getPlayerSkills(entityRef) ?: return
        skillRepository.savePlayerSkills(entityRef, skills)
        logger.info { "Saved skills for player ${event.playerRef.username} (${event.playerRef.uuid}) on disconnect." }
    }
}
