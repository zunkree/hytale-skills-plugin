package org.zunkree.hytale.plugins.skillsplugin.eventhandlers

import aster.amo.kytale.extension.componentType
import aster.amo.kytale.extension.info
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import org.zunkree.hytale.plugins.skillsplugin.SkillsComponent
import org.zunkree.hytale.plugins.skillsplugin.SkillsManager.getPlayerSkills
import org.zunkree.hytale.plugins.skillsplugin.SkillsPlugin

fun playerReadyEventHandler(event: PlayerReadyEvent) {
    val playerRef = event.playerRef
    SkillsPlugin.instance.logger.info { "Fetching skills for ${event.player.displayName}:${event.player.uuid}" }

    getPlayerSkills(playerRef) ?: run {
        SkillsPlugin.instance.logger.info { "No skills found for ${event.player.displayName}:${event.player.uuid}, initializing new skills component." }
        val store = playerRef.store
        val newSkills = SkillsComponent()
        store.addComponent(playerRef, componentType<SkillsComponent>(), newSkills)
        store.putComponent(playerRef, componentType<SkillsComponent>(), newSkills)
        SkillsPlugin.instance.logger.info { "Initialized skills for ${event.player.displayName}:${event.player.uuid}" }
    }
}