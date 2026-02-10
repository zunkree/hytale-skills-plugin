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

            val itemStack = player.inventory.itemInHand
            val item = itemStack?.item

            val skillType = if (item?.weapon != null) {
                val categories = item.categories ?: emptyArray()
                ctx.logger.info { "Player ${player.displayName} weapon categories: ${categories.toList()}" }
                categoryToSkillType(categories) ?: SkillsType.UNARMED
            } else {
                SkillsType.UNARMED
            }
            ctx.logger.info { "Mapped to skill type: $skillType" }

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

    private fun categoryToSkillType(categories: Array<String>): SkillsType? {
        for (cat in categories) {
            CATEGORY_SKILL_MAP[cat]?.let { return it }
        }
        return null
    }

    companion object {
        // Category ID → SkillsType mapping
        // TODO: update with actual category IDs from discovery logs
        val CATEGORY_SKILL_MAP = mapOf(
            "Swords" to SkillsType.SWORDS,
            "Daggers" to SkillsType.DAGGERS,
            "Axes" to SkillsType.AXES,
            "Bows" to SkillsType.BOWS,
            "Spears" to SkillsType.SPEARS,
            "Clubs" to SkillsType.CLUBS,
        )
    }
}
