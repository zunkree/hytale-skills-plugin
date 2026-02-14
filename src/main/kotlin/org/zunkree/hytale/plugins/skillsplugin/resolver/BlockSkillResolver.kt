package org.zunkree.hytale.plugins.skillsplugin.resolver

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

class BlockSkillResolver {
    fun resolve(blockId: String): SkillType? =
        when {
            blockId.startsWith("Rock_") || blockId.startsWith("Ore_") -> SkillType.MINING
            blockId.startsWith("Wood_") -> SkillType.WOODCUTTING
            else -> null
        }
}
