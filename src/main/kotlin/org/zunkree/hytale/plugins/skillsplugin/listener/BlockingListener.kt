package org.zunkree.hytale.plugins.skillsplugin.listener

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import org.zunkree.hytale.plugins.skillsplugin.config.ActionXpConfig
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import org.zunkree.hytale.plugins.skillsplugin.system.DamageContext
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService

class BlockingListener(
    private val xpService: XpService,
    private val actionXpConfig: ActionXpConfig,
    private val logger: HytaleLogger,
) {
    fun onPlayerBlock(ctx: DamageContext) {
        logger.debug { "Processing damage for blocking XP..." }
        val ref =
            ctx.playerRef?.reference ?: run {
                logger.debug { "Damage target is not a player, skipping blocking XP processing" }
                return
            }

        if (ctx.damage.getIfPresentMetaObject(Damage.BLOCKED) != true) {
            logger.debug { "Damage was not blocked, skipping blocking XP processing" }
            return
        }

        val baseXp = ctx.damage.initialAmount * actionXpConfig.blockingDamageMultiplier
        logger.debug {
            "Granting blocking XP: $baseXp (damage: ${ctx.damage.initialAmount}, " +
                "multiplier: ${actionXpConfig.blockingDamageMultiplier})"
        }
        xpService.grantXpAndSave(ref, SkillType.BLOCKING, baseXp, ctx.commandBuffer)
    }
}
