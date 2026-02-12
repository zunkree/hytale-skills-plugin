package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.hexweave.dsl.mechanics.HexweaveDamageContext
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.SkillsType
import org.zunkree.hytale.plugins.skillsplugin.SkillsXpService

class SkillsCombatListener {
    private val logger get() = SkillsPlugin.instance.logger
    private val config get() = SkillsPlugin.instance.config.xp.actionXp

    fun onPlayerDealDamage(ctx: HexweaveDamageContext) {
        val cause = DamageCause.getAssetMap().getAsset(ctx.damage.damageCauseIndex) ?: run {
            ctx.logger.debug { "Unknown damage cause index: ${ctx.damage.damageCauseIndex}" }
            return
        }
        ctx.logger.info { "Damage cause: $cause" }

        if (cause != DamageCause.PHYSICAL && cause != DamageCause.PROJECTILE) {
            ctx.logger.debug { "Unsupported damage cause: $cause" }
            return
        }
        ctx.logger.info { "Processing damage from cause: $cause" }

        val source = ctx.damage.source as? Damage.EntitySource ?: return
        val ref = source.ref
        val player = ref.store.getComponent(ref, componentType<Player>()) ?: run {
            ctx.logger.debug { "Damage source $ref is not a player" }
            return
        }
        ctx.logger.info { "Damage source is player: ${player.displayName}" }

        val skillType = player.inventory.itemInHand
            ?.let {
                ctx.logger.info { "Player ${player.displayName} is holding item: $it" }
                categoryToSkillType(it.item.id) ?: run {
                    ctx.logger.info { "No skill type mapping found for item categories: ${it.item.id}" }
                    return
                }
            }
            ?: SkillsType.UNARMED

        val baseXp = ctx.damage.initialAmount * config.combatDamageMultiplier
        ctx.logger.info { "Granting $skillType XP: $baseXp (damage: ${ctx.damage.initialAmount}, multiplier: ${config.combatDamageMultiplier})" }

        SkillsXpService.grantXpAndSave(ref, skillType, baseXp, ctx.commandBuffer)
    }

    private fun categoryToSkillType(itemId: String): SkillsType? {
        logger.info { "Mapping item '$itemId' to skill type based on categories" }
        for ((prefix, skillType) in WEAPON_PREFIX_TO_SKILL_TYPE) {
            if (itemId.startsWith(prefix)) {
                logger.info { "Item '$itemId' matched prefix '$prefix', mapped to skill type $skillType" }
                return skillType
            }
        }
        return null
    }

    companion object {
        // Weapon Id Prefix -> Skill Type mapping
        val WEAPON_PREFIX_TO_SKILL_TYPE = mapOf(
            "Weapon_Sword" to SkillsType.SWORDS,
            "Weapon_Longsword" to SkillsType.SWORDS,
            "Weapon_Daggers" to SkillsType.DAGGERS,
            "Weapon_Axe" to SkillsType.AXES,
            "Weapon_Battleaxe" to SkillsType.AXES,
            "Weapon_Shortbow" to SkillsType.BOWS,
            "Weapon_Crossbow" to SkillsType.BOWS,
            "Weapon_Spear" to SkillsType.SPEARS,
            "Weapon_Mace" to SkillsType.CLUBS,
            "Weapon_Club" to SkillsType.CLUBS,
        )
    }
}
