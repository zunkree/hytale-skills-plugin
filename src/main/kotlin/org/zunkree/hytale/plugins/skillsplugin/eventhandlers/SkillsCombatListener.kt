package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.hexweave.dsl.mechanics.HexweaveDamageContext
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.error
import aster.amo.kytale.extension.info

import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause

import org.zunkree.hytale.plugins.skillsplugin.SkillsManager
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.SkillsType
import org.zunkree.hytale.plugins.skillsplugin.SkillsXpCalculator
import org.zunkree.hytale.plugins.skillsplugin.SkillsXpService

class SkillsCombatListener {
    private val config get() = SkillsPlugin.Companion.instance.config.xp.actionXp

    fun onPlayerDealDamage(ctx: HexweaveDamageContext) {
        val cause = DamageCause.getAssetMap().getAsset(ctx.damage.damageCauseIndex)
            ?: run {
                ctx.logger.error { "Unknown damage cause index: ${ctx.damage.damageCauseIndex}" }
                return
            }
        ctx.logger.info { "Damage cause: $cause" }

        if (!(cause == DamageCause.PHYSICAL || cause == DamageCause.PROJECTILE)) {
            ctx.logger.error { "Unsupported damage cause: $cause" }
            return
        }
        ctx.logger.info { "Processing damage from cause: $cause" }

        val source = ctx.damage.source
        if (source is Damage.EntitySource) {
            val ref = source.ref
            val player = ref.store.getComponent(ref, componentType<Player>()) ?: run {
                ctx.logger.error { "Damage source $ref is not a player" }
                return
            }
            ctx.logger.info { "Damage source is player: ${player.displayName}" }

            val weapon = player.inventory.itemInHand?.itemId ?: run {
                ctx.logger.error { "Player ${player.displayName} has no item in hand for damage source $ref" }
                return
            }
            ctx.logger.info { "Player ${player.displayName} is using weapon: $weapon" }

            val skillType = when {
                weapon.startsWith("Weapon_Sword_") -> SkillsType.SWORDS
                weapon.startsWith("Weapon_Longsword_") -> SkillsType.SWORDS

                weapon.startsWith("Weapon_Axe_") -> SkillsType.AXES
                weapon.startsWith("Weapon_Battleaxe_") -> SkillsType.AXES

                weapon.startsWith("Weapon_Club_") -> SkillsType.CLUBS
                weapon.startsWith("Weapon_Mace_") -> SkillsType.CLUBS

                weapon.startsWith("Weapon_Daggers_") -> SkillsType.DAGGERS

                weapon.startsWith("Weapon_Spear_") -> SkillsType.SPEARS

                weapon.startsWith("Weapon_Shortbow_") -> SkillsType.BOWS
                weapon.startsWith("Weapon_Crossbow_") -> SkillsType.BOWS

                else -> SkillsType.UNARMED
            }
            ctx.logger.info { "Mapped weapon $weapon to skill type: $skillType" }

            val baseXp = ctx.damage.initialAmount * config.combatDamageMultiplier
            ctx.logger.info { "Calculated base XP for damage: $baseXp (initial damage: ${ctx.damage.initialAmount}, multiplier: ${config.combatDamageMultiplier})" }

            val result = SkillsXpService.grantXp(ref, skillType, baseXp)
            ctx.logger.info { "Granted XP for player ${player.displayName} in skill $skillType, result: $result" }

            result?.let {
                val skills = SkillsManager.getPlayerSkills(ref) ?: return
                SkillsManager.savePlayerSkills(ref, skills, ctx.commandBuffer)
                SkillsXpService.notifyLevelUp(ref, result)
            }
        }
    }
}
