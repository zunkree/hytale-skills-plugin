package org.zunkree.hytale.plugins.skillsplugin.effect

import aster.amo.hexweave.dsl.systems.context.EntityEventContext
import aster.amo.kytale.extension.debug
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.resolver.BlockSkillResolver

class GatheringEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger,
) {
    fun modifyBlockDamage(ctx: EntityEventContext<EntityStore, DamageBlockEvent>) {
        val ref = ctx.chunk.getReferenceTo(ctx.index)
        val blockType = ctx.event.blockType.id
        val skillType = blockSkillResolver.resolve(blockType) ?: return
        val level = skillRepository.getSkillLevel(ref, skillType) ?: return
        val multiplier = effectCalculator.getGatheringSpeedMultiplier(skillType, level)
        logger.debug { "Gathering damage modified: $skillType lv$level, multiplier=$multiplier" }
        ctx.event.damage *= multiplier.toFloat()
    }
}
