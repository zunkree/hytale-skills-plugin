package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.hexweave.internal.system.TickContext
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.movementStates
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.SkillsType
import org.zunkree.hytale.plugins.skillsplugin.SkillsXpService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class SkillsMovementListener {
    private val logger get() = SkillsPlugin.instance.logger
    private val config get() = SkillsPlugin.instance.config.xp.actionXp

    companion object {
        private val lastPositions = ConcurrentHashMap<UUID, Vector3d>()
        private val prevJumping = ConcurrentHashMap<UUID, Boolean>()

        private fun Vector3d.distanceTo(other: Vector3d): Double {
            val dx = x - other.x
            val dy = y - other.y
            val dz = z - other.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }
    }

    fun onTick(ctx: TickContext) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        val playerRef = ctx.store.getComponent(ref, componentType<PlayerRef>()) ?: run {
            logger.debug { "Entity $ref is not a player, skipping movement XP processing" }
            return
        }
        val entityId = playerRef.uuid

        val movementStates = playerRef.movementStates?.movementStates ?: run {
            logger.debug { "Player ${playerRef.username} has no movement states, skipping movement XP processing" }
            return
        }

        val transform = ctx.store.getComponent(ref, componentType<TransformComponent>()) ?: run {
            logger.debug { "Player ${playerRef.username} has no transform component, skipping movement XP processing" }
            return
        }
        val currentPosition = transform.position

        val lastPosition = lastPositions.put(entityId, Vector3d(currentPosition)) ?: run {
            logger.debug { "No last position for Player ${playerRef.username}, setting current $currentPosition" }
            return
        }

        val distance = currentPosition.distanceTo(lastPosition)

        if ((movementStates.running || movementStates.sprinting) && distance > 0) {
            val baseXp = distance * config.runningPerDistanceMultiplier
            logger.debug { "Player ${playerRef.username} is running/sprinting, granting XP: $baseXp for distance: $distance" }
            SkillsXpService.grantXpAndSave(ref, SkillsType.RUNNING, baseXp, ctx.commandBuffer)
        }

        if (movementStates.swimming && distance > 0) {
            val baseXp = distance * config.swimmingPerDistanceMultiplier
            logger.debug { "Player ${playerRef.username} is swimming, granting XP: $baseXp for distance: $distance" }
            SkillsXpService.grantXpAndSave(ref, SkillsType.SWIMMING, baseXp, ctx.commandBuffer)
        }

        val wasJumping = prevJumping.put(entityId, movementStates.jumping) ?: false
        if (movementStates.jumping && !wasJumping) {
            val baseXp = config.jumpingPerJumpMultiplier
            logger.debug { "Player ${playerRef.username} started jumping, granting XP for jump: $baseXp" }
            SkillsXpService.grantXpAndSave(ref, SkillsType.JUMPING, baseXp, ctx.commandBuffer)
        }

        if (movementStates.inFluid) {
            val baseXp = config.divingPerSecondMultiplier * ctx.deltaTime
            logger.debug { "Player ${playerRef.username} is diving in fluid, granting XP for diving: $baseXp" }
            SkillsXpService.grantXpAndSave(ref, SkillsType.DIVING, baseXp, ctx.commandBuffer)
        }

        if (movementStates.crouching && !movementStates.idle) {
            val baseXp = config.sneakingPerSecondMultiplier * ctx.deltaTime
            logger.debug { "Player ${playerRef.username} is sneaking, granting XP for sneaking: $baseXp" }
            SkillsXpService.grantXpAndSave(ref, SkillsType.SNEAKING, baseXp, ctx.commandBuffer)
        }
    }

    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        logger.debug { "Cleaning up movement listener state..." }
        val uuid = event.playerRef.uuid
        lastPositions.remove(uuid)
        prevJumping.remove(uuid)
    }
}
