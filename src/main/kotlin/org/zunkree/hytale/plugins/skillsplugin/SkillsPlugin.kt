package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.dsl.command
import aster.amo.kytale.dsl.event
import aster.amo.kytale.dsl.jsonConfig
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.debug
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
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

        // Composition root: wire all dependencies
        val xpCurve = XpCurve(config.xp)
        skillRepository = SkillRepository(logger)
        val xpService = XpService(skillRepository, xpCurve, config.general, logger)
        val actionXpConfig = config.xp.actionXp
        val weaponSkillResolver = WeaponSkillResolver()
        val blockSkillResolver = BlockSkillResolver()
        val movementXpPolicy = MovementXpPolicy(actionXpConfig)

        val combatListener = CombatListener(xpService, actionXpConfig, weaponSkillResolver, logger)
        val harvestListener = HarvestListener(xpService, actionXpConfig, blockSkillResolver, logger)
        val movementListener = MovementListener(xpService, movementXpPolicy, logger)
        val blockingListener = BlockingListener(xpService, actionXpConfig, logger)
        val playerLifecycleListener = PlayerLifecycleListener(skillRepository, activePlayers, logger)
        val commandHandler = SkillsCommandHandler(skillRepository, xpCurve, config.general, logger)

        val effectCalculator = SkillEffectCalculator(config.skillEffects, config.general.maxLevel)
        val combatEffectApplier = CombatEffectApplier(skillRepository, effectCalculator, weaponSkillResolver, logger)
        val movementEffectApplier = MovementEffectApplier(skillRepository, effectCalculator, logger)
        val statEffectApplier = StatEffectApplier(skillRepository, effectCalculator, logger)
        val gatheringEffectApplier =
            GatheringEffectApplier(skillRepository, effectCalculator, blockSkillResolver, logger)

        event<PlayerReadyEvent> { event -> playerLifecycleListener.onPlayerReady(event) }

        event<PlayerDisconnectEvent> { event ->
            playerLifecycleListener.onPlayerDisconnect(event)
            movementListener.onPlayerDisconnect(event)
            movementEffectApplier.onPlayerDisconnect(event.playerRef.uuid)
        }

        entityStoreRegistry.registerSystem(SkillEffectDamageSystem(combatEffectApplier, logger))
        entityStoreRegistry.registerSystem(CombatXpDamageSystem(combatListener, blockingListener, logger))

        registerHexweaveSystems(
            gatheringEffectApplier,
            movementEffectApplier,
            statEffectApplier,
            harvestListener,
            movementListener,
        )

        command("skills", "View your skill levels") {
            executesSync { ctx -> commandHandler.handleSkillsCommand(ctx) }
        }

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
        super.shutdown()
    }

    fun pluginVersion(): String = manifest.version.toString()
}
