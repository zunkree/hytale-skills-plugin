package org.zunkree.hytale.plugins.skillsplugin

enum class SkillsType(
    val displayName: String,
    val category: SkillsCategory
) {
    // Weapon skills
    SWORDS("Swords", SkillsCategory.WEAPON),
    DAGGERS("Daggers", SkillsCategory.WEAPON),
    AXES("Axes", SkillsCategory.WEAPON),
    BOWS("Bows", SkillsCategory.WEAPON),
    SPEARS("Spears", SkillsCategory.WEAPON),
    CLUBS("Clubs", SkillsCategory.WEAPON),
    UNARMED("Unarmed", SkillsCategory.WEAPON),

    // Utility skills
    BLOCKING("Blocking", SkillsCategory.UTILITY),
    MINING("Mining", SkillsCategory.GATHERING),
    WOODCUTTING("Woodcutting", SkillsCategory.GATHERING),
    RUNNING("Running", SkillsCategory.MOVEMENT),
    SWIMMING("Swimming", SkillsCategory.MOVEMENT),
    SNEAKING("Sneaking", SkillsCategory.MOVEMENT),
    JUMPING("Jumping", SkillsCategory.MOVEMENT);
}
