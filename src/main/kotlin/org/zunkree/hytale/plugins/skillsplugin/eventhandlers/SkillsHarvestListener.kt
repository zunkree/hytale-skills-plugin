package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.hexweave.dsl.systems.context.EntityEventContext
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.SkillsType
import org.zunkree.hytale.plugins.skillsplugin.SkillsXpService

class SkillsHarvestListener {
    private val logger get() = SkillsPlugin.instance.logger
    private val config get() = SkillsPlugin.instance.config.xp.actionXp

    fun onPlayerDamageBlock(ctx: EntityEventContext<EntityStore, DamageBlockEvent>) {
        logger.info { "Player is breaking a block, processing for skills XP..." }
        val ref = ctx.chunk.getReferenceTo(ctx.index)

        val blockId = ctx.event.blockType.id
        val skillType = blockToSkillType(blockId) ?: run {
            logger.debug { "No skill type mapping found for block ID: $blockId" }
            return
        }
        logger.debug { "Block $blockId maps to skill type: $skillType" }

        val multiplier = when (skillType) {
            SkillsType.MINING -> config.miningPerBlockMultiplier
            SkillsType.WOODCUTTING -> config.woodcuttingPerBlockMultiplier
            else -> return
        }
        logger.debug { "Using XP multiplier for $skillType: $multiplier with damage: ${ctx.event.damage}" }

        SkillsXpService.grantXpAndSave(ref, skillType, multiplier * ctx.event.damage, ctx.commandBuffer)
    }

    private fun blockToSkillType(blockId: String): SkillsType? = when {
        blockId.startsWith("Rock_") || blockId.startsWith("Ore_") -> SkillsType.MINING
        blockId.startsWith("Wood_") -> SkillsType.WOODCUTTING
        else -> null
    }
}
