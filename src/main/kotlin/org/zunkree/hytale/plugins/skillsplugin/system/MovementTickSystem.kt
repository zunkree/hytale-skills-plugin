package org.zunkree.hytale.plugins.skillsplugin.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.effect.MovementEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.StatEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.listener.MovementListener

class MovementTickSystem(
    private val movementListener: MovementListener,
    private val statEffectApplier: StatEffectApplier,
    private val movementEffectApplier: MovementEffectApplier,
    private val logger: HytaleLogger,
) : EntityTickingSystem<EntityStore>() {
    override fun getQuery(): Query<EntityStore> = PlayerRef.getComponentType()

    override fun tick(
        deltaTime: Float,
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
    ) {
        movementListener.onTick(index, chunk, store, commandBuffer, deltaTime)
        statEffectApplier.applyOxygenEffects(index, chunk, store)
        statEffectApplier.applyStaminaEffects(index, chunk, store)
        movementEffectApplier.applyMovementEffects(index, chunk, store)
    }
}
