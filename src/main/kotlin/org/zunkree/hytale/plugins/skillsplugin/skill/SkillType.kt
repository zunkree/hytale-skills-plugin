package org.zunkree.hytale.plugins.skillsplugin.skill

enum class SkillType(
    val displayName: String,
    val categories: Set<SkillCategory>,
) {
    // Combat skills
    SWORDS("Swords", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    DAGGERS("Daggers", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    AXES("Axes", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    BOWS("Bows", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    SPEARS("Spears", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    CLUBS("Clubs", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    UNARMED("Unarmed", setOf(SkillCategory.COMBAT, SkillCategory.WEAPON)),
    BLOCKING("Blocking", setOf(SkillCategory.COMBAT, SkillCategory.UTILITY)),

    // Gathering skills
    MINING("Mining", setOf(SkillCategory.GATHERING)),
    WOODCUTTING("Woodcutting", setOf(SkillCategory.GATHERING)),

    // Movement skills
    SWIMMING("Swimming", setOf(SkillCategory.MOVEMENT)),
    RUNNING("Running", setOf(SkillCategory.MOVEMENT)),
    DIVING("Diving", setOf(SkillCategory.MOVEMENT)),
    SNEAKING("Sneaking", setOf(SkillCategory.MOVEMENT)),
    JUMPING("Jumping", setOf(SkillCategory.MOVEMENT)),
}
