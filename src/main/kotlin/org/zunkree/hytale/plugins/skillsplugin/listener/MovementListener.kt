package org.zunkree.hytale.plugins.skillsplugin.listener

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
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

    fun onTick(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        deltaTime: Float,
    ) {
        val ref = chunk.getReferenceTo(index)
        val playerRef = store.getComponent(ref, PlayerRef.getComponentType()) ?: return
        val entityId = playerRef.uuid

        val movementStates =
            store.getComponent(ref, MovementStatesComponent.getComponentType())?.movementStates ?: return

        val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
        val currentPosition = transform.position

        val lastPosition = lastPositions.put(entityId, Vector3d(currentPosition))
        if (lastPosition == null) {
            logger.debug { "Movement: first position set for ${playerRef.username}" }
        }
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
                deltaTime = deltaTime.toDouble(),
            )

        if (grants.isEmpty() && distance > 0.01) {
            logger.debug {
                "Movement: no grants for ${playerRef.username}, " +
                    "distance=${"%.3f".format(distance)}, idle=${movementStates.idle}"
            }
        }
        for (grant in grants) {
            logger.debug {
                "Movement XP: player=${playerRef.username}, skill=${grant.skillType}, " +
                    "baseXp=${"%.4f".format(grant.baseXp)}, distance=${"%.3f".format(distance)}"
            }
            xpService.grantXpAndSave(ref, grant.skillType, grant.baseXp, commandBuffer)
        }
    }

    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        val uuid = event.playerRef.uuid
        lastPositions.remove(uuid)
        prevJumping.remove(uuid)
        logger.debug { "Movement: cleared position tracking for ${event.playerRef.username}" }
    }
}
