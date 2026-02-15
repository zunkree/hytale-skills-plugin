package org.zunkree.hytale.plugins.skillsplugin.effect

import aster.amo.hexweave.internal.system.TickContext
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.MovementStates
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MovementEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val logger: HytaleLogger,
) {
    private val lastSpeedMultiplier = ConcurrentHashMap<UUID, Float>()
    private val lastJumpMultiplier = ConcurrentHashMap<UUID, Float>()

    fun applyMovementEffects(ctx: TickContext) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        val playerUuid =
            ctx.store.getComponent(ref, componentType<PlayerRef>())?.uuid ?: return
        val movementManager = ctx.store.getComponent(ref, componentType<MovementManager>()) ?: return
        val movementStates =
            ctx.store.getComponent(ref, componentType<MovementStatesComponent>())?.movementStates ?: return

        applySpeedEffect(ref, movementManager, movementStates, playerUuid)
        applyJumpEffect(ref, movementManager, playerUuid)
    }

    fun onPlayerDisconnect(uuid: UUID) {
        lastSpeedMultiplier.remove(uuid)
        lastJumpMultiplier.remove(uuid)
        logger.debug { "Cleared movement effect state for $uuid" }
    }

    private fun applySpeedEffect(
        ref: Ref<EntityStore>,
        movementManager: MovementManager,
        movementStates: MovementStates,
        playerUuid: UUID,
    ) {
        val prevMultiplier = lastSpeedMultiplier.getOrDefault(playerUuid, 1.0f)

        val speedSkill =
            when {
                movementStates.running || movementStates.sprinting -> SkillType.RUNNING
                movementStates.crouching -> SkillType.SNEAKING
                movementStates.swimming -> SkillType.SWIMMING
                else -> null
            }

        val newMultiplier =
            if (speedSkill != null) {
                val level = skillRepository.getSkillLevel(ref, speedSkill) ?: 0
                if (level > 0) {
                    1.0f + effectCalculator.getSpeedBonus(speedSkill, level).toFloat()
                } else {
                    1.0f
                }
            } else {
                1.0f
            }

        if (prevMultiplier != newMultiplier) {
            logger.debug { "Speed effect: $speedSkill, multiplier $prevMultiplier -> $newMultiplier" }
            movementManager.settings.baseSpeed =
                movementManager.settings.baseSpeed / prevMultiplier * newMultiplier
            lastSpeedMultiplier[playerUuid] = newMultiplier
        } else if (speedSkill != null) {
            logger.debug { "Speed effect: $speedSkill unchanged, multiplier=$prevMultiplier" }
        }
    }

    private fun applyJumpEffect(
        ref: Ref<EntityStore>,
        movementManager: MovementManager,
        playerUuid: UUID,
    ) {
        val prevMultiplier = lastJumpMultiplier.getOrDefault(playerUuid, 1.0f)

        val jumpingLevel = skillRepository.getSkillLevel(ref, SkillType.JUMPING) ?: 0
        val newMultiplier =
            if (jumpingLevel > 0) {
                effectCalculator.getJumpHeightMultiplier(jumpingLevel).toFloat()
            } else {
                1.0f
            }

        if (prevMultiplier != newMultiplier) {
            logger.debug { "Jump effect: lv$jumpingLevel, multiplier $prevMultiplier -> $newMultiplier" }
            movementManager.settings.jumpForce =
                movementManager.settings.jumpForce / prevMultiplier * newMultiplier
            lastJumpMultiplier[playerUuid] = newMultiplier
        } else if (jumpingLevel > 0) {
            logger.debug { "Jump effect: lv$jumpingLevel unchanged, multiplier=$prevMultiplier" }
        }
    }
}
