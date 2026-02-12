package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.hexweave.dsl.mechanics.HexweaveDamageContext
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.SkillsType
import org.zunkree.hytale.plugins.skillsplugin.SkillsXpService

class SkillsBlockingListener {
    private val config get() = SkillsPlugin.instance.config.xp.actionXp
    private val logger get() = SkillsPlugin.instance.logger

    fun onPlayerBlock(ctx: HexweaveDamageContext) {
        logger.info { "Processing damage for blocking XP..." }
        val ref = ctx.playerRef?.reference ?: run {
            logger.debug { "Damage target is not a player, skipping blocking XP processing" }
            return
        }

        if (ctx.damage.getIfPresentMetaObject(Damage.BLOCKED) != true) {
            logger.debug { "Damage was not blocked, skipping blocking XP processing" }
            return
        }

        val baseXp = ctx.damage.initialAmount * config.blockingDamageMultiplier
        logger.debug { "Granting blocking XP: $baseXp (initial damage: ${ctx.damage.initialAmount}, multiplier: ${config.blockingDamageMultiplier})" }
        SkillsXpService.grantXpAndSave(ref, SkillsType.BLOCKING, baseXp, ctx.commandBuffer)
    }
}
