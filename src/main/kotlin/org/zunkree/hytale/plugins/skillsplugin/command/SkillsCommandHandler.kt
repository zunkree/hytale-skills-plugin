package org.zunkree.hytale.plugins.skillsplugin.command

import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.error
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.config.GeneralConfig
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.xp.XpCurve

class SkillsCommandHandler(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val generalConfig: GeneralConfig,
    private val logger: HytaleLogger,
) {
    fun handleSkillsCommand(
        ctx: CommandContext,
        ref: Ref<EntityStore>,
    ) {
        logger.debug { "Player executed /skills command." }

        val player =
            ref.store.getComponent(ref, Player.getComponentType()) ?: run {
                logger.error { "Player component not found for ref: $ref" }
                ctx.sendMessage(Message.raw("Error: Player data not found."))
                return
            }

        val skills = skillRepository.getPlayerSkills(ref)
        logger.debug { "Fetched skills for ${player.displayName}: ${skills?.skills?.keys ?: "No skills"}" }

        val message =
            buildString {
                append("Your Skills:\n")
                if (skills == null || skills.skills.isEmpty()) {
                    append("No skills yet. Start playing to gain experience!")
                } else {
                    for ((skillType, skillData) in skills.skills) {
                        val progress =
                            xpCurve.levelProgress(
                                skillData.level,
                                skillData.totalXP,
                                generalConfig.maxLevel,
                            )
                        logger.debug {
                            "Processing skill ${skillType.displayName} for ${player.displayName}: " +
                                "Level ${skillData.level}, XP ${skillData.totalXP}, Next: ${(progress * 100).toInt()}%"
                        }
                        val pct = (progress * 100).toInt()
                        append("${skillType.displayName}: Level ${skillData.level} ($pct% to next level)\n")
                    }
                }
            }
        logger.debug { "Sending skills message to ${player.displayName}" }
        ctx.sendMessage(Message.raw(message.trim()))
    }
}
