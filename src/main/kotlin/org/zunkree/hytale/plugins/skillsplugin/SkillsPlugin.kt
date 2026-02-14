package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.hexweave.enableHexweave
import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.dsl.event
import aster.amo.kytale.dsl.jsonConfig
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfig
import org.zunkree.hytale.plugins.skillsplugin.listener.BlockingListener
import org.zunkree.hytale.plugins.skillsplugin.listener.CombatListener
import org.zunkree.hytale.plugins.skillsplugin.listener.HarvestListener
import org.zunkree.hytale.plugins.skillsplugin.listener.MovementListener
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import org.zunkree.hytale.plugins.skillsplugin.resolver.BlockSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.resolver.MovementXpPolicy
import org.zunkree.hytale.plugins.skillsplugin.resolver.WeaponSkillResolver
import org.zunkree.hytale.plugins.skillsplugin.skill.PlayerSkillsComponent
import org.zunkree.hytale.plugins.skillsplugin.xp.XpCurve
import org.zunkree.hytale.plugins.skillsplugin.xp.XpService

class SkillsPlugin(
    init: JavaPluginInit,
) : KotlinPlugin(init) {
    companion object {
        lateinit var instance: SkillsPlugin
            private set
    }

    val config by jsonConfig<SkillsConfig>("config") { SkillsConfig() }
    private lateinit var skillRepository: SkillRepository

    override fun setup() {
        super.setup()
        instance = this

        val version = pluginVersion()
        logger.info { "HytaleSkills v$version loading..." }

        validateConfig()

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

        event<PlayerDisconnectEvent> { event ->
            movementListener.onPlayerDisconnect(event)
        }

        enableHexweave {
            systems {
                damageSystem("skills-combat-xp") {
                    dependencies { after<DamageSystems.ApplyDamage>() }
                    filter { !it.isCancelled }
                    onDamage {
                        combatListener.onPlayerDealDamage(this)
                        blockingListener.onPlayerBlock(this)
                    }
                }

                entityEventSystem<EntityStore, DamageBlockEvent>("skills-gathering-xp") {
                    query { componentType<PlayerRef>() }
                    filter { it.blockType != BlockType.EMPTY }
                    onEvent { harvestListener.onPlayerDamageBlock(this) }
                }

                tickSystem("skills-movement-xp") {
                    query { componentType<PlayerRef>() }
                    onTick { movementListener.onTick(this) }
                }
            }
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
            logger.info { "Weapon: $id, categories: ${item.categories?.toList()}" }
        }

        logger.info { "HytaleSkills started." }
    }

    override fun shutdown() {
        super.shutdown()
    }

    private fun validateConfig() {
        with(config.general) {
            require(maxLevel > 0) { "maxLevel must be > 0, got $maxLevel" }
        }
        with(config.xp) {
            require(baseXpPerAction > 0) { "baseXpPerAction must be > 0, got $baseXpPerAction" }
            require(xpScaleFactor >= 0) { "xpScaleFactor must be >= 0, got $xpScaleFactor" }
            require(globalXpMultiplier > 0) { "globalXpMultiplier must be > 0, got $globalXpMultiplier" }
            require(restedBonusMultiplier >= 1.0) { "restedBonusMultiplier must be >= 1.0, got $restedBonusMultiplier" }
        }
        with(config.deathPenalty) {
            require(penaltyPercentage in 0.0..1.0) {
                "penaltyPercentage must be in 0.0..1.0, got $penaltyPercentage"
            }
            require(immunityDurationSeconds >= 0) {
                "immunityDurationSeconds must be >= 0, got $immunityDurationSeconds"
            }
        }
    }

    fun pluginVersion(): String = manifest.version.toString()
}
