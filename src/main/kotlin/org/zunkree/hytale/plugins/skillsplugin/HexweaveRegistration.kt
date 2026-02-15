package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.hexweave.enableHexweave
import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.extension.componentType
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.effect.GatheringEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.MovementEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.StatEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.listener.HarvestListener
import org.zunkree.hytale.plugins.skillsplugin.listener.MovementListener

fun KotlinPlugin.registerHexweaveSystems(
    gatheringEffectApplier: GatheringEffectApplier,
    movementEffectApplier: MovementEffectApplier,
    statEffectApplier: StatEffectApplier,
    harvestListener: HarvestListener,
    movementListener: MovementListener,
) {
    enableHexweave {
        systems {
            entityEventSystem<EntityStore, DamageBlockEvent>("skills-gathering") {
                query { componentType<PlayerRef>() }
                filter { it.blockType != BlockType.EMPTY }
                onEvent {
                    harvestListener.onPlayerDamageBlock(this)
                    gatheringEffectApplier.modifyBlockDamage(this)
                }
            }

            tickSystem("skills-movement") {
                query { componentType<PlayerRef>() }
                onTick {
                    movementListener.onTick(this)
                    statEffectApplier.applyOxygenEffects(this)
                    statEffectApplier.applyStaminaEffects(this)
                    movementEffectApplier.applyMovementEffects(this)
                }
            }
        }
    }
}
