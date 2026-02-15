package org.zunkree.hytale.plugins.skillsplugin.listener

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.resolver.BlockSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService

class HarvestListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val blockSkillResolver: BlockSkillResolver,
    private val logger: HytaleLogger,
) {
    fun onPlayerDamageBlock(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        event: DamageBlockEvent,
    ) {
        val ref = chunk.getReferenceTo(index)
        val blockId = event.blockType.id
        val skillType =
            blockSkillResolver.resolve(blockId) ?: run {
                logger.debug { "Harvest: block $blockId not a recognized gathering block, skipping" }
                return
            }

        val multiplier =
            when (skillType) {
                SkillType.MINING -> actionXpConfig.miningPerBlockMultiplier
                SkillType.WOODCUTTING -> actionXpConfig.woodcuttingPerBlockMultiplier
                else -> {
                    logger.debug { "Harvest: skill $skillType has no configured multiplier, skipping" }
                    return
                }
            }
        val baseXp = multiplier * event.damage
        logger.debug {
            "Harvest XP: skill=$skillType, block=$blockId, " +
                "damage=${event.damage}, multiplier=$multiplier, baseXp=$baseXp"
        }

        xpService.grantXpAndSave(ref, skillType, baseXp, commandBuffer)
    }
}
