package org.zunkree.hytale.plugins.skillsplugin.listener

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.resolver.WeaponSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.system.DamageContext
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService

class CombatListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val weaponSkillResolver: WeaponSkillResolver,
    private val logger: HytaleLogger,
) {
    fun onPlayerDealDamage(ctx: DamageContext) {
        val cause =
            DamageCause.getAssetMap().getAsset(ctx.damage.damageCauseIndex) ?: run {
                logger.debug { "Unknown damage cause index: ${ctx.damage.damageCauseIndex}" }
                return
            }

        logger.debug { "Processing combat XP for damage cause: $cause, with id: ${cause.id}" }
        if (cause.id != "Physical" && cause.id != "Projectile") {
            logger.debug { "Damage cause $cause is not physical or projectile, skipping combat XP processing" }
            return
        }

        val source =
            ctx.damage.source as? Damage.EntitySource ?: run {
                logger.debug { "Combat: damage source is not an entity, skipping" }
                return
            }
        val ref = source.ref
        val player =
            ref.store.getComponent(ref, Player.getComponentType()) ?: run {
                logger.debug { "Combat: attacker is not a player, skipping" }
                return
            }

        val skillType =
            player.inventory.itemInHand
                ?.let { item ->
                    weaponSkillResolver.resolve(item.item.id) ?: run {
                        logger.debug { "Combat: item ${item.item.id} not a recognized weapon, skipping" }
                        return
                    }
                }
                ?: SkillType.UNARMED

        val baseXp = ctx.damage.initialAmount * actionXpConfig.combatDamageMultiplier
        logger.debug {
            "Combat XP: player=${player.displayName}, skill=$skillType, " +
                "damage=${ctx.damage.initialAmount}, baseXp=$baseXp"
        }

        xpService.grantXpAndSave(ref, skillType, baseXp, ctx.commandBuffer)
    }
}
