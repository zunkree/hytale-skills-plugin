package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.hexweave.enableHexweave
import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.dsl.command
import aster.amo.kytale.dsl.event
import aster.amo.kytale.dsl.jsonConfig
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.event.events.ShutdownEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.SkillsManager.getPlayerSkills
import org.zunkree.hytale.plugins.skillsplugin.commandhandlers.handleSkillsCommand
import org.zunkree.hytale.plugins.skillsplugin.eventhandlers.SkillsCombatListener
import org.zunkree.hytale.plugins.skillsplugin.eventhandlers.playerReadyEventHandler
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SkillsPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    companion object {
        lateinit var instance: SkillsPlugin
            private set
    }

    val config by jsonConfig<SkillsConfig>("config") { SkillsConfig() }
    val activePlayers = ConcurrentHashMap<UUID, Ref<EntityStore>>()

    override fun setup() {
        super.setup()
        instance = this

        val version = pluginVersion()
        logger.info { "HytaleSkills v$version loading..." }

        event<PlayerReadyEvent> { event -> playerReadyEventHandler(event) }

        event<PlayerDisconnectEvent> { event ->
            val entityRef = activePlayers.remove(event.playerRef.uuid) ?: return@event
            val skills = getPlayerSkills(entityRef) ?: return@event
            SkillsManager.savePlayerSkills(entityRef, skills)
        }

        val combatListener = SkillsCombatListener()

        enableHexweave {
            systems {
                damageSystem("skills-combat-xp") {
                    dependencies { after<DamageSystems.ApplyDamage>() }
                    filter { !it.isCancelled }
                    onDamage { combatListener.onPlayerDealDamage(this) }
                }
            }
        }

        command("skills", "View your skill levels") {
            executesSync { ctx -> handleSkillsCommand(ctx) }
        }

        SkillsComponent.componentType = entityStoreRegistry.registerComponent(
            SkillsComponent::class.java,
            "SkillsComponent",
            SkillsComponent.CODEC
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
        logger.info { "HytaleSkills shutting down, saving ${activePlayers.size} player(s)..." }
        activePlayers.values.forEach { entityRef ->
            val skills = getPlayerSkills(entityRef) ?: return@forEach
            SkillsManager.savePlayerSkills(entityRef, skills)
        }
        activePlayers.clear()
        super.shutdown()
    }

    fun pluginVersion(): String = manifest.version.toString()
}
