package org.zunkree.hytale.plugins.skillsplugin.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.effect.GatheringEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.listener.HarvestListener

class BlockDamageXpSystem(
    private val harvestListener: HarvestListener,
    private val gatheringEffectApplier: GatheringEffectApplier,
    private val logger: HytaleLogger,
) : EntityEventSystem<EntityStore, DamageBlockEvent>(DamageBlockEvent::class.java) {
    override fun getQuery(): Query<EntityStore> = PlayerRef.getComponentType()

    override fun handle(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        event: DamageBlockEvent,
    ) {
        if (event.blockType == BlockType.EMPTY) {
            logger.debug { "BlockDamageXpSystem: empty block type, skipping" }
            return
        }
        harvestListener.onPlayerDamageBlock(index, chunk, commandBuffer, event)
        gatheringEffectApplier.modifyBlockDamage(index, chunk, event)
    }
}
