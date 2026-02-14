package org.zunkree.hytale.plugins.skillsplugin.listener

import aster.amo.hexweave.dsl.mechanics.HexweaveDamageContext
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.resolver.WeaponSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService

class CombatListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger,
) {
    fun onPlayerDealDamage(ctx: HexweaveDamageContext) {
        val cause =
            DamageCause.getAssetMap().getAsset(ctx.damage.damageCauseIndex) ?: run {
                ctx.logger.debug { "Unknown damage cause index: ${ctx.damage.damageCauseIndex}" }
                return
            }

        if (cause.id != "PHYSICAL" && cause.id != "PROJECTILE") {
            ctx.logger.debug { "Damage cause $cause is not physical or projectile, skipping combat XP processing" }
            return
        }

        val source = ctx.damage.source as? Damage.EntitySource ?: return
        val ref = source.ref
        val player = ref.store.getComponent(ref, componentType<Player>()) ?: return

        val skillType =
            player.inventory.itemInHand
                ?.let { weaponSkillResolver.resolve(it.item.id) ?: return }
                ?: SkillType.UNARMED

        val baseXp = ctx.damage.initialAmount * actionXpConfig.combatDamageMultiplier
        ctx.logger.debug { "Granting $skillType XP: $baseXp for ${player.displayName}" }

        xpService.grantXpAndSave(ref, skillType, baseXp, ctx.commandBuffer)
    }
}
