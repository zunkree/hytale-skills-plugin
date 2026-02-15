package org.zunkree.hytale.plugins.skillsplugin.effect

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.resolver.BlockSkillResolver

class GatheringEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger,
) {
    fun modifyBlockDamage(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        event: DamageBlockEvent,
    ) {
        val ref = chunk.getReferenceTo(index)
        val blockType = event.blockType.id
        val skillType =
            blockSkillResolver.resolve(blockType) ?: run {
                logger.debug { "GatheringEffect: block $blockType not recognized, no speed bonus" }
                return
            }
        val level =
            skillRepository.getSkillLevel(ref, skillType) ?: run {
                logger.debug { "GatheringEffect: no skill data for $skillType, no speed bonus" }
                return
            }
        val multiplier = effectCalculator.getGatheringSpeedMultiplier(skillType, level)
        logger.debug { "Gathering damage modified: $skillType lv$level, multiplier=$multiplier" }
        event.damage *= multiplier.toFloat()
    }
}
