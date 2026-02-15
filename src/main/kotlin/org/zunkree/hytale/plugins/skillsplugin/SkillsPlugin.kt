package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.dsl.event
import aster.amo.kytale.dsl.jsonConfig
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.command.SkillsCommand
import org.zunkree.hytale.plugins.skillsplugin.command.SkillsCommandHandler
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfig
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfigValidator
import org.zunkree.hytale.plugins.skillsplugin.effect.CombatEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.GatheringEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.MovementEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.SkillEffectCalculator
import org.zunkree.hytale.plugins.skillsplugin.effect.StatEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.listener.BlockingListener
import org.zunkree.hytale.plugins.skillsplugin.listener.CombatListener
import org.zunkree.hytale.plugins.skillsplugin.listener.HarvestListener
import org.zunkree.hytale.plugins.skillsplugin.listener.MovementListener
import org.zunkree.hytale.plugins.skillsplugin.listener.PlayerLifecycleListener
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.resolver.BlockSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.resolver.MovementXpPolicy
import org.zunkree.hytale.plugins.skillsplugin.resolver.WeaponSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.PlayerSkillsComponent
import org.zunkree.hytale.plugins.skillsplugin.system.CombatXpDamageSystem
import org.zunkree.hytale.plugins.skillsplugin.system.SkillEffectDamageSystem
import org.zunkree.hytale.plugins.skillsplugin.xp.XpCurve
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SkillsPlugin(
    init: JavaPluginInit,
) : KotlinPlugin(init) {
    val config by jsonConfig<SkillsConfig>("config") { SkillsConfig() }
    val activePlayers = ConcurrentHashMap<UUID, Ref<EntityStore>>()
    private lateinit var skillRepository: SkillRepository

    override fun setup() {
        super.setup()

        val version = pluginVersion()
        logger.info { "HytaleSkills v$version loading..." }

        SkillsConfigValidator.validate(config)
        logConfigSummary()

        // Sub-loggers for granular log-level control per subsystem
        val effectsLogger = logger.getSubLogger("Effects")
        val xpLogger = logger.getSubLogger("XP")
        val lifecycleLogger = logger.getSubLogger("Lifecycle")
        val persistenceLogger = logger.getSubLogger("Persistence")
        val commandLogger = logger.getSubLogger("Command")

        // Composition root: wire all dependencies
        val xpCurve = XpCurve(config.xp)
        skillRepository = SkillRepository(persistenceLogger)
        val xpService = XpService(skillRepository, xpCurve, config.general, xpLogger)
        val actionXpConfig = config.xp.actionXp
        val weaponSkillResolver = WeaponSkillResolver()
        val blockSkillResolver = BlockSkillResolver()
        val movementXpPolicy = MovementXpPolicy(actionXpConfig)

        val combatListener = CombatListener(xpService, actionXpConfig, weaponSkillResolver, xpLogger)
        val harvestListener = HarvestListener(xpService, actionXpConfig, blockSkillResolver, xpLogger)
        val movementListener = MovementListener(xpService, movementXpPolicy, xpLogger)
        val blockingListener = BlockingListener(xpService, actionXpConfig, xpLogger)
        val playerLifecycleListener = PlayerLifecycleListener(skillRepository, activePlayers, lifecycleLogger)
        val commandHandler = SkillsCommandHandler(skillRepository, xpCurve, config.general, commandLogger)

        val effectCalculator = SkillEffectCalculator(config.skillEffects, config.general.maxLevel)
        val combatEffectApplier =
            CombatEffectApplier(skillRepository, effectCalculator, weaponSkillResolver, effectsLogger)
        val movementEffectApplier = MovementEffectApplier(skillRepository, effectCalculator, effectsLogger)
        val statEffectApplier = StatEffectApplier(skillRepository, effectCalculator, effectsLogger)
        val gatheringEffectApplier =
            GatheringEffectApplier(skillRepository, effectCalculator, blockSkillResolver, effectsLogger)

        event<PlayerReadyEvent> { event -> playerLifecycleListener.onPlayerReady(event) }

        event<PlayerDisconnectEvent> { event ->
            playerLifecycleListener.onPlayerDisconnect(event)
            movementListener.onPlayerDisconnect(event)
            movementEffectApplier.onPlayerDisconnect(event.playerRef.uuid)
        }

        entityStoreRegistry.registerSystem(SkillEffectDamageSystem(combatEffectApplier, effectsLogger))
        entityStoreRegistry.registerSystem(CombatXpDamageSystem(combatListener, blockingListener, xpLogger))

        registerHexweaveSystems(
            gatheringEffectApplier,
            movementEffectApplier,
            statEffectApplier,
            harvestListener,
            movementListener,
        )

        commandRegistry.registerCommand(SkillsCommand(commandHandler))

        PlayerSkillsComponent.componentType =
            entityStoreRegistry.registerComponent(
                PlayerSkillsComponent::class.java,
                "SkillsComponent",
                PlayerSkillsComponent.CODEC,
            )

        logger.info { "HytaleSkills v$version loaded." }
    }

    override fun start() {
        super.start()

        val allItems = Item.getAssetMap().getAssetMap()
        val weaponItems = allItems.filter { (_, item) -> item.weapon != null }
        weaponItems.forEach { (id, item) ->
            logger.debug { "Weapon: $id, categories: ${item.categories?.toList()}" }
        }

        logger.info { "HytaleSkills started." }
    }

    override fun shutdown() {
        logger.info { "HytaleSkills shutting down, saving ${activePlayers.size} player(s)..." }
        activePlayers.values.forEach { entityRef ->
            val skills = skillRepository.getPlayerSkills(entityRef) ?: return@forEach
            skillRepository.savePlayerSkills(entityRef, skills)
        }
        activePlayers.clear()
        logger.info { "HytaleSkills shutdown complete." }
        super.shutdown()
    }

    fun pluginVersion(): String = manifest.version.toString()

    private fun logConfigSummary() {
        logger.debug {
            "Config loaded: maxLevel=${config.general.maxLevel}, " +
                "globalXpMultiplier=${config.xp.globalXpMultiplier}, " +
                "baseXpPerAction=${config.xp.baseXpPerAction}, " +
                "deathPenalty=${config.deathPenalty.enabled} " +
                "(${(config.deathPenalty.penaltyPercentage * 100).toInt()}%)"
        }
    }
}
