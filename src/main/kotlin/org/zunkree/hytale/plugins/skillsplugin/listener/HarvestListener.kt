package org.zunkree.hytale.plugins.skillsplugin.listener

import aster.amo.hexweave.dsl.systems.context.EntityEventContext
import aster.amo.kytale.extension.debug
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.resolver.BlockSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService

class HarvestListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger,
) {
    fun onPlayerDamageBlock(ctx: EntityEventContext<EntityStore, DamageBlockEvent>) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        val blockId = ctx.event.blockType.id
        val skillType = blockSkillResolver.resolve(blockId) ?: return

        val multiplier =
            when (skillType) {
                SkillType.MINING -> actionXpConfig.miningPerBlockMultiplier
                SkillType.WOODCUTTING -> actionXpConfig.woodcuttingPerBlockMultiplier
                else -> return
            }
        logger.debug { "Granting $skillType XP for block $blockId with multiplier $multiplier" }

        xpService.grantXpAndSave(ref, skillType, multiplier * ctx.event.damage, ctx.commandBuffer)
    }
}
