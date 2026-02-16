package org.zunkree.hytale.plugins.skillsplugin.xp

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.EventTitleUtil
import com.hypixel.hytale.server.core.util.NotificationUtil
import org.zunkree.hytale.plugins.skillsplugin.config.GeneralConfig
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.extension.info
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class LevelUpResult(
    val skillType: SkillType,
    val oldLevel: Int,
    val newLevel: Int,
)

internal data class XpGrantResult(
    val newLevel: Int,
    val newTotalXP: Double,
    val levelUp: LevelUpResult?,
)

internal data class XpNotificationState(
    var lastSentTime: Long = 0L,
    var accumulatedXp: Double = 0.0,
)

internal fun computeXpGrant(
    currentLevel: Int,
    currentTotalXP: Double,
    skillType: SkillType,
    baseXp: Double,
    maxLevel: Int,
    xpCurve: XpCurve,
    isRested: Boolean = false,
): XpGrantResult? {
    if (currentLevel >= maxLevel) return null

    val xpGain = xpCurve.calculateXpGain(baseXp, isRested)
    val newTotalXP = currentTotalXP + xpGain

    var newLevel = currentLevel

    while (newLevel < maxLevel) {
        val xpForNext = xpCurve.cumulativeXpForLevel(newLevel + 1)
        if (newTotalXP >= xpForNext) {
            newLevel++
        } else {
            break
        }
    }

    val levelUp =
        if (newLevel > currentLevel) {
            LevelUpResult(skillType, currentLevel, newLevel)
        } else {
            null
        }

    return XpGrantResult(newLevel, newTotalXP, levelUp)
}

class XpService(
    private val skillRepository: SkillRepository,
    private val xpCurve: XpCurve,
    private val generalConfig: GeneralConfig,
    private val logger: HytaleLogger,
) {
    private val xpNotifications = ConcurrentHashMap<Pair<UUID, SkillType>, XpNotificationState>()
    private val xpNotificationCooldownMs = 10_000L

    fun grantXp(
        ref: Ref<EntityStore>,
        skillType: SkillType,
        baseXp: Double,
        isRested: Boolean = false,
    ): LevelUpResult? {
        val skills =
            skillRepository.getPlayerSkills(ref)
                ?: run {
                    logger.info { "Failed to load player skills from ${ref.store}" }
                    return null
                }
        val skillData = skills.getSkill(skillType)

        if (skillData.level >= generalConfig.maxLevel) {
            logger.debug { "XP grant skipped: $skillType already at max level ${generalConfig.maxLevel}" }
            return null
        }

        val result =
            computeXpGrant(
                skillData.level,
                skillData.totalXP,
                skillType,
                baseXp,
                generalConfig.maxLevel,
                xpCurve,
                isRested,
            ) ?: return null

        skillData.level = result.newLevel
        skillData.totalXP = result.newTotalXP
        logger.debug {
            "XP granted: skill=$skillType, baseXp=$baseXp, " +
                "level=${result.newLevel}, totalXP=${"%.2f".format(result.newTotalXP)}"
        }
        if (result.levelUp != null) {
            logger.info {
                "LEVEL UP: skill=${result.levelUp.skillType}, " +
                    "${result.levelUp.oldLevel} -> ${result.levelUp.newLevel}"
            }
        }
        return result.levelUp
    }

    fun grantXpAndSave(
        ref: Ref<EntityStore>,
        skillType: SkillType,
        baseXp: Double,
        commandBuffer: CommandBuffer<EntityStore>,
        isRested: Boolean = false,
    ) {
        val levelUp = grantXp(ref, skillType, baseXp, isRested)
        val skills = skillRepository.getPlayerSkills(ref) ?: return
        skillRepository.savePlayerSkills(ref, skills, commandBuffer)
        if (levelUp != null) {
            notifyLevelUp(ref, levelUp)
        }
        notifyXpGain(ref, skillType, xpCurve.calculateXpGain(baseXp, isRested))
    }

    fun notifyLevelUp(
        ref: Ref<EntityStore>,
        result: LevelUpResult,
    ) {
        if (!generalConfig.showLevelUpNotifications) {
            logger.debug { "Level-up notification skipped: showLevelUpNotifications=false" }
            return
        }

        val playerRef =
            ref.store.getComponent(ref, PlayerRef.getComponentType())
                ?: run {
                    logger.info { "Failed to load PlayerRef from ${ref.store}" }
                    return
                }
        EventTitleUtil.showEventTitleToPlayer(
            playerRef,
            Message.raw("Level Up!"),
            Message.raw("${result.skillType.displayName}: Level ${result.newLevel}"),
            true,
        )
    }

    private fun notifyXpGain(
        ref: Ref<EntityStore>,
        skillType: SkillType,
        xpGain: Double,
    ) {
        if (!generalConfig.showXpGainNotifications) return

        val playerRef =
            ref.store.getComponent(ref, PlayerRef.getComponentType()) ?: return
        val playerId = playerRef.uuid
        val key = playerId to skillType
        val state = xpNotifications.getOrPut(key) { XpNotificationState() }
        state.accumulatedXp += xpGain

        val now = System.currentTimeMillis()
        if (now - state.lastSentTime < xpNotificationCooldownMs) return
        state.lastSentTime = now

        val totalGain = state.accumulatedXp
        state.accumulatedXp = 0.0

        val skills = skillRepository.getPlayerSkills(ref)
        val skillData = skills?.getSkill(skillType)
        val primary = Message.raw("+${"%.1f".format(totalGain)} ${skillType.displayName} XP")

        if (skillData != null) {
            val progress =
                xpCurve.levelProgress(
                    skillData.level,
                    skillData.totalXP,
                    generalConfig.maxLevel,
                )
            val progressPct = (progress * 100).toInt()
            val secondary = Message.raw("Level ${skillData.level} — $progressPct%")
            NotificationUtil.sendNotification(
                playerRef.packetHandler,
                primary,
                secondary,
                NotificationStyle.Success,
            )
        } else {
            NotificationUtil.sendNotification(
                playerRef.packetHandler,
                primary,
                NotificationStyle.Success,
            )
        }
        logger.debug {
            "XP notification sent: +${"%.1f".format(totalGain)} ${skillType.displayName} to $playerId"
        }
    }

    fun onPlayerDisconnect(playerId: UUID) {
        xpNotifications.keys.removeIf { it.first == playerId }
    }
}
