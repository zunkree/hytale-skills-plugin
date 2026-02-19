package org.zunkree.hytale.plugins.skillsplugin.effect

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.MovementStates
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.extension.isSubmerged
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StatEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val logger: HytaleLogger,
) {
    private val lastStaminaActive = ConcurrentHashMap<UUID, Boolean>()
    private val lastOxygenActive = ConcurrentHashMap<UUID, Boolean>()

    fun applyStaminaEffects(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
    ) {
        val ref = chunk.getReferenceTo(index)
        val playerUuid =
            store.getComponent(ref, PlayerRef.getComponentType())?.uuid ?: return
        val movementStates =
            store.getComponent(ref, MovementStatesComponent.getComponentType())?.movementStates ?: return

        val statMap = store.getComponent(ref, EntityStatMap.getComponentType()) ?: return
        val staminaIndex = DefaultEntityStatTypes.getStamina()
        val reduction = resolveStaminaReduction(ref, movementStates)

        if (reduction != null) {
            val wasActive = lastStaminaActive.put(playerUuid, true) ?: false
            if (!wasActive) {
                logger.debug {
                    "Stamina modifier applied: ${reduction.first} lv${reduction.second}, " +
                        "reduction=${reduction.third}"
                }
            }
            statMap.putModifier(
                staminaIndex,
                STAMINA_MODIFIER_KEY,
                StaticModifier(
                    Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.MULTIPLICATIVE,
                    (1.0 - reduction.third).toFloat(),
                ),
            )
            return
        }

        val wasActive = lastStaminaActive.put(playerUuid, false) ?: false
        if (wasActive) {
            logger.debug { "Stamina modifier removed" }
        }
        statMap.removeModifier(staminaIndex, STAMINA_MODIFIER_KEY)
    }

    private fun resolveStaminaReduction(
        ref: Ref<EntityStore>,
        movementStates: MovementStates,
    ): Triple<SkillType, Int, Double>? {
        if (!movementStates.sprinting) return null
        val staminaSkill =
            when {
                movementStates.swimming -> SkillType.SWIMMING
                movementStates.running -> SkillType.RUNNING
                else -> return null
            }
        val level = skillRepository.getSkillLevel(ref, staminaSkill) ?: 0
        if (level <= 0) return null
        val reduction = effectCalculator.getStaminaReduction(staminaSkill, level)
        if (reduction <= 0.0) return null
        return Triple(staminaSkill, level, reduction)
    }

    fun applyOxygenEffects(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
    ) {
        val ref = chunk.getReferenceTo(index)
        val playerUuid =
            store.getComponent(ref, PlayerRef.getComponentType())?.uuid ?: return

        val statMap = store.getComponent(ref, EntityStatMap.getComponentType()) ?: return
        val oxygenIndex = DefaultEntityStatTypes.getOxygen()

        if (isSubmerged(ref, store)) {
            val divingLevel = skillRepository.getSkillLevel(ref, SkillType.DIVING) ?: 0
            if (divingLevel > 0) {
                val oxygenBonus = effectCalculator.getDivingOxygenBonus(divingLevel)
                if (oxygenBonus > 0.0) {
                    val wasActive = lastOxygenActive.put(playerUuid, true) ?: false
                    if (!wasActive) {
                        logger.debug {
                            "Oxygen modifier applied: DIVING lv$divingLevel, bonus=$oxygenBonus"
                        }
                    }
                    statMap.putModifier(
                        oxygenIndex,
                        OXYGEN_MODIFIER_KEY,
                        StaticModifier(
                            Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.MULTIPLICATIVE,
                            (1.0 + oxygenBonus).toFloat(),
                        ),
                    )
                    return
                }
            }
        }

        val wasActive = lastOxygenActive.put(playerUuid, false) ?: false
        if (wasActive) {
            logger.debug { "Oxygen modifier removed" }
        }
        statMap.removeModifier(oxygenIndex, OXYGEN_MODIFIER_KEY)
    }

    fun onPlayerDisconnect(uuid: UUID) {
        lastStaminaActive.remove(uuid)
        lastOxygenActive.remove(uuid)
    }

    companion object {
        private const val STAMINA_MODIFIER_KEY = "skills-stamina"
        private const val OXYGEN_MODIFIER_KEY = "skills-oxygen"
    }
}
