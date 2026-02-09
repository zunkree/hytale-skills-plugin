package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.error
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
import org.zunkree.hytale.plugins.skillsplugin.SkillsManager
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.SkillsType

class SkillsCombatListener {
    private val config get() = SkillsPlugin.Companion.instance.config.xp.actionXp

    fun onPlayerDealDamage(event: Damage) {
        val cause = DamageCause.getAssetMap().getAsset(event.damageCauseIndex)
            ?: run {
                SkillsPlugin.instance.logger.error { "Unknown damage cause index: ${event.damageCauseIndex}" }
                return
            }
        if (!(cause == DamageCause.PHYSICAL || cause == DamageCause.PROJECTILE)) {
            SkillsPlugin.instance.logger.error { "Unsupported damage cause: $cause" }
            return
        }

        val source = event.source
        if (source is Damage.EntitySource) {
            val ref = source.ref
            val player = ref.store.getComponent(ref, componentType<Player>()) ?: run {
                SkillsPlugin.instance.logger.error { "Damage source $ref is not a player" }
                return
            }

            val skills = SkillsManager.getPlayerSkills(ref) ?: run {
                SkillsPlugin.instance.logger.error { "Failed to load player skills for damage source $ref" }
                return
            }

            val weapon = player.inventory.itemInHand?.itemId ?: run {
                SkillsPlugin.instance.logger.error { "Player ${player.displayName} has no item in hand for damage source $ref" }
                return
            }

            val skillType = when {
                weapon.startsWith("Weapon_Sword_") -> SkillsType.SWORDS
                else -> SkillsType.UNARMED
            }
        }
    }
}