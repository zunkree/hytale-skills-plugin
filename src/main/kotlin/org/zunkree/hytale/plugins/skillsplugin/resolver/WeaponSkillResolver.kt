package org.zunkree.hytale.plugins.skillsplugin.resolver

import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType

class WeaponSkillResolver {
    fun resolve(itemId: String): SkillType? {
        for ((prefix, skillType) in WEAPON_PREFIX_TO_SKILL_TYPE) {
            if (itemId.startsWith(prefix)) {
                return skillType
            }
        }
        return null
    }

    companion object {
        val WEAPON_PREFIX_TO_SKILL_TYPE =
            mapOf(
                "Weapon_Sword" to SkillType.SWORDS,
                "Weapon_Longsword" to SkillType.SWORDS,
                "Weapon_Daggers" to SkillType.DAGGERS,
                "Weapon_Axe" to SkillType.AXES,
                "Weapon_Battleaxe" to SkillType.AXES,
                "Weapon_Shortbow" to SkillType.BOWS,
                "Weapon_Crossbow" to SkillType.BOWS,
                "Weapon_Spear" to SkillType.SPEARS,
                "Weapon_Mace" to SkillType.CLUBS,
                "Weapon_Club" to SkillType.CLUBS,
            )
    }
}
