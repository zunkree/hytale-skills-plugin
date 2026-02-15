package org.zunkree.hytale.plugins.skillsplugin.system

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

data class DamageContext(
    val damage: Damage,
    val commandBuffer: CommandBuffer<EntityStore>,
    val playerRef: PlayerRef?,
    val logger: HytaleLogger,
)
