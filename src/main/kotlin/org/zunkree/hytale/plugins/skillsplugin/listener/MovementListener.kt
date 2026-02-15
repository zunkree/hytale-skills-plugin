package org.zunkree.hytale.plugins.skillsplugin.listener

import aster.amo.hexweave.internal.system.TickContext
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.movementStates
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import org.zunkree.hytale.plugins.skillsplugin.resolver.MovementXpPolicy
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private fun Vector3d.distanceTo(other: Vector3d): Double {
    val dx = x - other.x
    val dy = y - other.y
    val dz = z - other.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}

class MovementListener(
    private val xpService: XpService,
    private val movementXpPolicy: MovementXpPolicy,
    private val logger: HytaleLogger,
) {
    private val lastPositions = ConcurrentHashMap<UUID, Vector3d>()
    private val prevJumping = ConcurrentHashMap<UUID, Boolean>()

    fun onTick(ctx: TickContext) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        val playerRef = ctx.store.getComponent(ref, componentType<PlayerRef>()) ?: return
        val entityId = playerRef.uuid

        val movementStates = playerRef.movementStates?.movementStates ?: return

        val transform = ctx.store.getComponent(ref, componentType<TransformComponent>()) ?: return
        val currentPosition = transform.position

        val lastPosition = lastPositions.put(entityId, Vector3d(currentPosition))
        val distance = lastPosition?.let { currentPosition.distanceTo(it) } ?: 0.0
        val wasJumping = prevJumping.put(entityId, movementStates.jumping) ?: false

        val grants =
            movementXpPolicy.calculateGrants(
                isRunning = movementStates.running,
                isSprinting = movementStates.sprinting,
                isSwimming = movementStates.swimming,
                isJumping = movementStates.jumping,
                wasJumping = wasJumping,
                isInFluid = movementStates.inFluid,
                isCrouching = movementStates.crouching,
                isIdle = movementStates.idle,
                distance = distance,
                deltaTime = ctx.deltaTime.toDouble(),
            )

        for (grant in grants) {
            logger.debug { "Player ${playerRef.username} granted ${grant.skillType} XP: ${grant.baseXp}" }
            xpService.grantXpAndSave(ref, grant.skillType, grant.baseXp, ctx.commandBuffer)
        }
    }

    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        val uuid = event.playerRef.uuid
        lastPositions.remove(uuid)
        prevJumping.remove(uuid)
    }
}
