package org.zunkree.hytale.plugins.skillsplugin.commandhandlers

import aster.amo.kytale.extension.error
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.entity.entities.Player

import org.zunkree.hytale.plugins.skillsplugin.SkillsManager.getPlayerSkills
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin

fun handleSkillsCommand(ctx: CommandContext) {
    val logger = SkillsPlugin.instance.logger

    if (!ctx.isPlayer) {
        ctx.sendMessage(Message.raw("Only players can use this command."))
        return
    }
    logger.info { "Player executed /skills command." }

    val player = ctx.sender() as Player
    val world = player.world ?: run {
        logger.error { "Player ${player.displayName} is not in a world." }
        ctx.sendMessage(Message.raw("An error occurred while fetching your skills."))
        return
    }

    world.execute {
        val playerRef = player.reference ?: run {
            logger.error { "Player reference is null for ${player.displayName}" }
            ctx.sendMessage(Message.raw("An error occurred while fetching your skills."))
            return@execute
        }
        val skills = getPlayerSkills(playerRef)
        logger.info { "Fetched skills for ${player.displayName}: ${skills?.skills?.keys ?: "No skills"}" }

        val message = buildString {
            append("Your Skills:\n")
            if (skills == null || skills.skills.isEmpty()) {
                append("No skills yet. Start playing to gain experience!")
            } else {
                for ((skillName, skillData) in skills.skills) {
                    logger.info { "Processing skill $skillName for ${player.displayName}: Level ${skillData.level}, Total XP ${skillData.totalXP}" }
                    append("$skillName: Level ${skillData.level} (${(skillData.getLevelProgress() * 100).toInt()}% to next level)\n")
                }
            }
        }
        logger.info { "Sending skills message to ${player.displayName}" }
        player.sendMessage(Message.raw(message.trim()))
    }
}
