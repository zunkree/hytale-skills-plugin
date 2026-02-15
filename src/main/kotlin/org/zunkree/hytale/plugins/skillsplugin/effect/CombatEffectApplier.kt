package org.zunkree.hytale.plugins.skillsplugin.effect

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.resolver.WeaponSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.system.DamageContext

class CombatEffectApplier(
    private val skillRepository: SkillRepository,
    private val effectCalculator: SkillEffectCalculator,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger,
) {
    fun modifyOutgoingDamage(ctx: DamageContext) {
        val source =
            ctx.damage.source as? Damage.EntitySource ?: run {
                logger.debug { "CombatEffect: damage source is not entity, skipping" }
                return
            }
        val ref = source.ref
        val player =
            ref.store.getComponent(ref, componentType<Player>()) ?: run {
                logger.debug { "CombatEffect: attacker is not a player, skipping" }
                return
            }
        val skillType =
            player.inventory.itemInHand?.let { weaponSkillResolver.resolve(it.item.id) } ?: SkillType.UNARMED
        val level = skillRepository.getSkillLevel(ref, skillType) ?: return
        if (level <= 0) {
            logger.debug { "CombatEffect: $skillType level is 0, no damage modifier" }
            return
        }
        val oldDamage = ctx.damage.amount
        val multiplier = effectCalculator.getDamageMultiplier(skillType, level)
        ctx.damage.amount = (ctx.damage.amount * multiplier).toFloat()
        logger.debug {
            "CombatEffect: $skillType lv$level, multiplier=$multiplier, " +
                "damage=$oldDamage -> ${ctx.damage.amount}"
        }
    }

    fun modifyBlockingStamina(ctx: DamageContext) {
        val ref =
            ctx.playerRef?.reference ?: run {
                logger.debug { "CombatEffect: blocking target is not a player, skipping" }
                return
            }

        if (ctx.damage.getIfPresentMetaObject(Damage.BLOCKED) != true) return

        val level = skillRepository.getSkillLevel(ref, SkillType.BLOCKING) ?: return
        if (level <= 0) {
            logger.debug { "CombatEffect: Blocking level is 0, no stamina reduction" }
            return
        }

        val staminaReduction = effectCalculator.getStaminaReduction(SkillType.BLOCKING, level)
        if (staminaReduction > 0.0) {
            logger.debug { "Blocking stamina reduced: lv$level, reduction=$staminaReduction" }
            val currentStaminaMult = ctx.damage.getIfPresentMetaObject(Damage.STAMINA_DRAIN_MULTIPLIER) ?: 1.0f
            ctx.damage.putMetaObject(
                Damage.STAMINA_DRAIN_MULTIPLIER,
                currentStaminaMult * (1.0f - staminaReduction.toFloat()),
            )
        }
    }
}
