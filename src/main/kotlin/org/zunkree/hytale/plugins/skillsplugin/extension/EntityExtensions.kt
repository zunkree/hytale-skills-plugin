package org.zunkree.hytale.plugins.skillsplugin.extension

import com.hypixel.hytale.component.ComponentAccessor
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.util.MathUtil
import com.hypixel.hytale.protocol.BlockMaterial
import com.hypixel.hytale.server.core.entity.LivingEntity
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

fun isSubmerged(
    ref: Ref<EntityStore>,
    store: ComponentAccessor<EntityStore>,
): Boolean {
    val packed = LivingEntity.getPackedMaterialAndFluidAtBreathingHeight(ref, store)
    val material = BlockMaterial.VALUES[MathUtil.unpackLeft(packed)]
    val fluidId = MathUtil.unpackRight(packed)
    return !(material == BlockMaterial.Empty && fluidId == 0)
}
