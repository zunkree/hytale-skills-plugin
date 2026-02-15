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
        val source = ctx.damage.source as? Damage.EntitySource ?: return
        val ref = source.ref
        val player = ref.store.getComponent(ref, componentType<Player>()) ?: return
        val skillType =
            player.inventory.itemInHand?.let { weaponSkillResolver.resolve(it.item.id) } ?: SkillType.UNARMED
        val level = skillRepository.getSkillLevel(ref, skillType) ?: return
        if (level <= 0) return
        val multiplier = effectCalculator.getDamageMultiplier(skillType, level)
        logger.debug { "Damage modified: $skillType lv$level, multiplier=$multiplier" }
        ctx.damage.amount = (ctx.damage.amount * multiplier).toFloat()
    }

    fun modifyBlockingStamina(ctx: DamageContext) {
        val ref = ctx.playerRef?.reference ?: return

        if (ctx.damage.getIfPresentMetaObject(Damage.BLOCKED) != true) return

        val level = skillRepository.getSkillLevel(ref, SkillType.BLOCKING) ?: return
        if (level <= 0) return

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
