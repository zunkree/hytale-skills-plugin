package org.zunkree.hytale.plugins.skillsplugin.system

import aster.amo.kytale.extension.componentType
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.dependency.Dependency
import com.hypixel.hytale.component.dependency.Order
import com.hypixel.hytale.component.dependency.SystemDependency
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.listener.BlockingListener
import org.zunkree.hytale.plugins.skillsplugin.listener.CombatListener

class CombatXpDamageSystem(
    private val combatListener: CombatListener,
    private val blockingListener: BlockingListener,
    private val logger: HytaleLogger,
) : DamageEventSystem() {
    override fun getQuery(): Query<EntityStore> = AllLegacyLivingEntityTypesQuery.INSTANCE

    override fun getDependencies(): Set<Dependency<EntityStore>> =
        setOf(
            SystemDependency(Order.AFTER, DamageSystems.ApplyDamage::class.java),
        )

    override fun handle(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        damage: Damage,
    ) {
        if (damage.isCancelled) return
        val ref = chunk.getReferenceTo(index)
        val playerRef = store.getComponent(ref, componentType<PlayerRef>())
        val ctx = DamageContext(damage, commandBuffer, playerRef, logger)
        combatListener.onPlayerDealDamage(ctx)
        blockingListener.onPlayerBlock(ctx)
    }
}
