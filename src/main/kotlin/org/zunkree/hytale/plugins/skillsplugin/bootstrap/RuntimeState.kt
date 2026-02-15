package org.zunkree.hytale.plugins.skillsplugin.bootstrap

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.zunkree.hytale.plugins.skillsplugin.persistence.SkillRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RuntimeState(
    val activePlayers: ConcurrentHashMap<UUID, Ref<EntityStore>>,
    val skillRepository: SkillRepository,
)
