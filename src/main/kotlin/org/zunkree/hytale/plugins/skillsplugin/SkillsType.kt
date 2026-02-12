package org.zunkree.hytale.plugins.skillsplugin

enum class SkillsType(
    val displayName: String,
    val categories: Set<SkillsCategory>
) {
    // Combat skills
    SWORDS("Swords", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    DAGGERS("Daggers", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    AXES("Axes", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    BOWS("Bows", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    SPEARS("Spears", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    CLUBS("Clubs", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    UNARMED("Unarmed", setOf(SkillsCategory.COMBAT, SkillsCategory.WEAPON)),
    BLOCKING("Blocking", setOf(SkillsCategory.COMBAT, SkillsCategory.UTILITY)),

    // Gathering skills
    MINING("Mining", setOf(SkillsCategory.GATHERING)),
    WOODCUTTING("Woodcutting", setOf(SkillsCategory.GATHERING)),

    // Movement skills
    SWIMMING("Swimming", setOf(SkillsCategory.MOVEMENT)),
    RUNNING("Running", setOf(SkillsCategory.MOVEMENT)),
    DIVING("Diving", setOf(SkillsCategory.MOVEMENT)),
    SNEAKING("Sneaking", setOf(SkillsCategory.MOVEMENT)),
    JUMPING("Jumping", setOf(SkillsCategory.MOVEMENT));
}
