package org.zunkree.hytale.plugins.skillsplugin.effect

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

class StatEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val logger: HytaleLogger,
) {
    fun applyStaminaEffects(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
    ) {
        val ref = chunk.getReferenceTo(index)
        val movementStates =
            store.getComponent(ref, MovementStatesComponent.getComponentType())?.movementStates ?: return

        val statMap = store.getComponent(ref, EntityStatMap.getComponentType()) ?: return
        val staminaIndex = DefaultEntityStatTypes.getStamina()

        if (movementStates.sprinting) {
            val runningLevel = skillRepository.getSkillLevel(ref, SkillType.RUNNING) ?: 0
            if (runningLevel > 0) {
                val staminaReduction = effectCalculator.getStaminaReduction(SkillType.RUNNING, runningLevel)
                if (staminaReduction > 0.0) {
                    logger.debug { "Stamina modifier applied: RUNNING lv$runningLevel, reduction=$staminaReduction" }
                    statMap.putModifier(
                        staminaIndex,
                        STAMINA_MODIFIER_KEY,
                        StaticModifier(
                            Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.MULTIPLICATIVE,
                            (1.0 - staminaReduction).toFloat(),
                        ),
                    )
                    return
                }
            }
        }

        logger.debug { "Stamina modifier removed (not sprinting or level 0)" }
        statMap.removeModifier(staminaIndex, STAMINA_MODIFIER_KEY)
    }

    fun applyOxygenEffects(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
    ) {
        val ref = chunk.getReferenceTo(index)
        val movementStates =
            store.getComponent(ref, MovementStatesComponent.getComponentType())?.movementStates ?: return

        val statMap = store.getComponent(ref, EntityStatMap.getComponentType()) ?: return
        val oxygenIndex = DefaultEntityStatTypes.getOxygen()

        if (movementStates.inFluid) {
            val divingLevel = skillRepository.getSkillLevel(ref, SkillType.DIVING) ?: 0
            if (divingLevel > 0) {
                val oxygenBonus = effectCalculator.getDivingOxygenBonus(divingLevel)
                if (oxygenBonus > 0.0) {
                    logger.debug { "Oxygen modifier applied: DIVING lv$divingLevel, bonus=$oxygenBonus" }
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

        logger.debug { "Oxygen modifier removed (not in fluid or level 0)" }
        statMap.removeModifier(oxygenIndex, OXYGEN_MODIFIER_KEY)
    }

    companion object {
        private const val STAMINA_MODIFIER_KEY = "skills-stamina"
        private const val OXYGEN_MODIFIER_KEY = "skills-oxygen"
    }
}
