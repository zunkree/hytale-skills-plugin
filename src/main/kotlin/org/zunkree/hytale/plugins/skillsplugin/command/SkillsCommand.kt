package org.zunkree.hytale.plugins.skillsplugin.command

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class SkillsCommand(
    private val handler: SkillsCommandHandler,
) : AbstractPlayerCommand("skills", "View your skill levels and progress") {
    override fun canGeneratePermission(): Boolean = false

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World,
    ) {
        handler.handleSkillsCommand(context, ref)
    }
}
