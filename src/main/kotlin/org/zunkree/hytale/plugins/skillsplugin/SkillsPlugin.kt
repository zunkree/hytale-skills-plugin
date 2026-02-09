package org.zunkree.hytale.plugins.skillsplugin

import aster.amo.kytale.KotlinPlugin
import aster.amo.kytale.dsl.command
import aster.amo.kytale.dsl.event
import aster.amo.kytale.dsl.jsonConfig
import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import org.zunkree.hytale.plugins.skillsplugin.SkillsManager.getPlayerSkills
import org.zunkree.hytale.plugins.skillsplugin.commandhandlers.handleSkillsCommand
import org.zunkree.hytale.plugins.skillsplugin.eventhandlers.playerReadyEventHandler

class SkillsPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    companion object {
        lateinit var instance: SkillsPlugin
            private set
    }

    val config by jsonConfig<SkillsConfig>("config") { SkillsConfig() }

    override fun setup() {
        super.setup()
        instance = this

        val version = pluginVersion()
        logger.info { "HytaleSkills v$version loading..." }

        event<PlayerReadyEvent> { event -> playerReadyEventHandler(event)}

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
        logger.info { "HytaleSkills started." }
    }

    override fun shutdown() {
        logger.info { "HytaleSkills shutting down." }
        super.shutdown()
    }

    fun pluginVersion(): String = manifest.version.toString()
}
