package org.zunkree.hytale.plugins.skillsplugin.bootstrap

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin
import org.zunkree.hytale.plugins.skillsplugin.command.SkillsCommand
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfig
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfigValidator
import org.zunkree.hytale.plugins.skillsplugin.effect.CombatEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.GatheringEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.MovementEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.effect.SkillEffectCalculator
import org.zunkree.hytale.plugins.skillsplugin.effect.StatEffectApplier
import org.zunkree.hytale.plugins.skillsplugin.extension.debug
import org.zunkree.hytale.plugins.skillsplugin.extension.info
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
import org.zunkree.hytale.plugins.skillsplugin.system.BlockDamageXpSystem
import org.zunkree.hytale.plugins.skillsplugin.system.CombatXpDamageSystem
import org.zunkree.hytale.plugins.skillsplugin.system.MovementTickSystem
import org.zunkree.hytale.plugins.skillsplugin.system.SkillEffectDamageSystem
import org.zunkree.hytale.plugins.skillsplugin.xp.XpCurve
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PluginApplication(
    private val plugin: SkillsPlugin,
) {
    private lateinit var runtimeState: RuntimeState
    private lateinit var movementListener: MovementListener
    private lateinit var movementEffectApplier: MovementEffectApplier

    fun setup() {
        val logger = plugin.logger
        val version = plugin.manifest.version.toString()
        logger.info { "HytaleSkills v$version loading..." }

        val configPath = plugin.dataDirectory.resolve("config.json")
        if (!Files.exists(configPath)) {
            logger.info { "Config not found, writing defaults." }
            plugin.configRef.save().join()
        }
        val config: SkillsConfig = plugin.configRef.load().join()
        SkillsConfigValidator.validate(config)
        logConfigSummary(config)

        val services = createServices(config)
        registerAll(config, services)

        runtimeState = RuntimeState(services.activePlayers, services.skillRepository)
        logger.info { "HytaleSkills v$version loaded." }
    }

    private fun createServices(config: SkillsConfig): ServiceGraph {
        val logger = plugin.logger
        val effectsLogger = logger.getSubLogger("Effects")
        val xpLogger = logger.getSubLogger("XP")
        val lifecycleLogger = logger.getSubLogger("Lifecycle")
        val persistenceLogger = logger.getSubLogger("Persistence")
        val commandLogger = logger.getSubLogger("Command")

        val activePlayers = ConcurrentHashMap<UUID, Ref<EntityStore>>()
        val xpCurve = XpCurve(config.xp)
        val skillRepository = SkillRepository(persistenceLogger)
        val xpService = XpService(skillRepository, xpCurve, config.general, xpLogger)
        val actionXpConfig = config.xp.actionXp
        val weaponSkillResolver = WeaponSkillResolver()
        val blockSkillResolver = BlockSkillResolver()
        val movementXpPolicy = MovementXpPolicy(actionXpConfig)

        val combatListener = CombatListener(xpService, actionXpConfig, weaponSkillResolver, xpLogger)
        val harvestListener = HarvestListener(xpService, actionXpConfig, blockSkillResolver, xpLogger)
        movementListener = MovementListener(xpService, movementXpPolicy, xpLogger)
        val blockingListener = BlockingListener(xpService, actionXpConfig, xpLogger)
        val playerLifecycleListener = PlayerLifecycleListener(skillRepository, activePlayers, lifecycleLogger)

        val effectCalculator = SkillEffectCalculator(config.skillEffects, config.general.maxLevel)
        val combatEffectApplier =
            CombatEffectApplier(skillRepository, effectCalculator, weaponSkillResolver, effectsLogger)
        movementEffectApplier = MovementEffectApplier(skillRepository, effectCalculator, effectsLogger)
        val statEffectApplier = StatEffectApplier(skillRepository, effectCalculator, effectsLogger)
        val gatheringEffectApplier =
            GatheringEffectApplier(skillRepository, effectCalculator, blockSkillResolver, effectsLogger)

        return ServiceGraph(
            activePlayers,
            skillRepository,
            xpCurve,
            xpService,
            combatListener,
            harvestListener,
            blockingListener,
            playerLifecycleListener,
            combatEffectApplier,
            statEffectApplier,
            gatheringEffectApplier,
            commandLogger,
        )
    }

    private fun registerAll(
        config: SkillsConfig,
        s: ServiceGraph,
    ) {
        plugin.eventRegistry.register(PlayerReadyEvent::class.java) { event ->
            s.playerLifecycleListener.onPlayerReady(event)
        }
        plugin.eventRegistry.register(PlayerDisconnectEvent::class.java) { event ->
            s.playerLifecycleListener.onPlayerDisconnect(event)
            movementListener.onPlayerDisconnect(event)
            movementEffectApplier.onPlayerDisconnect(event.playerRef.uuid)
            s.statEffectApplier.onPlayerDisconnect(event.playerRef.uuid)
            s.xpService.onPlayerDisconnect(event.playerRef.uuid)
        }

        val log = plugin.logger
        plugin.entityStoreRegistry.registerSystem(SkillEffectDamageSystem(s.combatEffectApplier, log))
        plugin.entityStoreRegistry.registerSystem(
            CombatXpDamageSystem(s.combatListener, s.blockingListener, log),
        )
        plugin.entityStoreRegistry.registerSystem(
            BlockDamageXpSystem(s.harvestListener, s.gatheringEffectApplier, log),
        )
        plugin.entityStoreRegistry.registerSystem(
            MovementTickSystem(movementListener, s.statEffectApplier, movementEffectApplier, log),
        )

        plugin.commandRegistry.registerCommand(
            SkillsCommand(s.skillRepository, s.xpCurve, config.general, s.commandLogger),
        )

        PlayerSkillsComponent.componentType =
            plugin.entityStoreRegistry.registerComponent(
                PlayerSkillsComponent::class.java,
                "SkillsComponent",
                PlayerSkillsComponent.CODEC,
            )
    }

    fun start() {
        val logger = plugin.logger
        val allItems = Item.getAssetMap().getAssetMap()
        val weaponItems = allItems.filter { (_, item) -> item.weapon != null }
        weaponItems.forEach { (id, item) ->
            logger.debug { "Weapon: $id, categories: ${item.categories?.toList()}" }
        }

        logger.info { "HytaleSkills started." }
    }

    fun shutdown() {
        val logger = plugin.logger
        if (!::runtimeState.isInitialized) {
            logger.info { "HytaleSkills shutdown (setup was incomplete, nothing to save)." }
            return
        }
        logger.info { "HytaleSkills shutting down, saving ${runtimeState.activePlayers.size} player(s)..." }
        runtimeState.activePlayers.values.forEach { entityRef ->
            val skills = runtimeState.skillRepository.getPlayerSkills(entityRef) ?: return@forEach
            runtimeState.skillRepository.savePlayerSkills(entityRef, skills)
        }
        runtimeState.activePlayers.clear()
        logger.info { "HytaleSkills shutdown complete." }
    }

    private fun logConfigSummary(config: SkillsConfig) {
        plugin.logger.debug {
            "Config loaded: maxLevel=${config.general.maxLevel}, " +
                "globalXpMultiplier=${config.xp.globalXpMultiplier}, " +
                "baseXpPerAction=${config.xp.baseXpPerAction}, " +
                "deathPenalty=${config.deathPenalty.enabled} " +
                "(${(config.deathPenalty.penaltyPercentage * 100).toInt()}%)"
        }
    }
}

private class ServiceGraph(
    val activePlayers: ConcurrentHashMap<UUID, Ref<EntityStore>>,
    val skillRepository: SkillRepository,
    val xpCurve: XpCurve,
    val xpService: XpService,
    val combatListener: CombatListener,
    val harvestListener: HarvestListener,
    val blockingListener: BlockingListener,
    val playerLifecycleListener: PlayerLifecycleListener,
    val combatEffectApplier: CombatEffectApplier,
    val statEffectApplier: StatEffectApplier,
    val gatheringEffectApplier: GatheringEffectApplier,
    val commandLogger: HytaleLogger,
)
