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
import org.zunkree.hytale.plugins.skillsplugin.extension.isSubmerged
import org.zunkree.hytale.plugins.skillsplugin.resolver.MovementXpPolicy
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
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
    private val activeGrants = ConcurrentHashMap<UUID, MutableSet<SkillType>>()
    private val accumulatedXp = ConcurrentHashMap<Pair<UUID, SkillType>, Double>()

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
                isSprinting = movementStates.sprinting,
                isSwimming = movementStates.swimming,
                isJumping = movementStates.jumping,
                wasJumping = wasJumping,
                isDiving = movementStates.inFluid && isSubmerged(ref, store),
                isCrouching = movementStates.crouching,
                isIdle = movementStates.idle,
                distance = distance,
                deltaTime = deltaTime.toDouble(),
            )

        val currentSkills = grants.map { it.skillType }.toSet()
        val previousSkills = activeGrants[entityId] ?: emptySet()

        for (skill in currentSkills - previousSkills) {
            logger.debug { "Movement XP started: $skill for ${playerRef.username}" }
        }
        for (skill in previousSkills - currentSkills) {
            val total = accumulatedXp.remove(entityId to skill) ?: 0.0
            logger.debug {
                "Movement XP ended: $skill for ${playerRef.username}, " +
                    "total=+${"%.1f".format(total)} XP"
            }
        }

        activeGrants[entityId] = currentSkills.toMutableSet()

        for (grant in grants) {
            accumulatedXp.merge(entityId to grant.skillType, grant.baseXp) { a, b -> a + b }
            xpService.grantXpAndSave(ref, grant.skillType, grant.baseXp, commandBuffer)
        }
    }

    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        val uuid = event.playerRef.uuid
        lastPositions.remove(uuid)
        prevJumping.remove(uuid)
        activeGrants.remove(uuid)
        accumulatedXp.keys.removeIf { it.first == uuid }
        logger.debug { "Movement: cleared state for ${event.playerRef.username}" }
    }
}
