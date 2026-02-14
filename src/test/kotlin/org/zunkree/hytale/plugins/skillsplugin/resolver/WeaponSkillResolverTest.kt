package org.zunkree.hytale.plugins.skillsplugin.resolver

import org.junit.jupiter.api.Test
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeaponSkillResolverTest {
    private val resolver = WeaponSkillResolver()

    @Test
    fun `resolves sword to SWORDS`() {
        assertEquals(SkillType.SWORDS, resolver.resolve("Weapon_Sword_Iron"))
    }

    @Test
    fun `resolves longsword to SWORDS`() {
        assertEquals(SkillType.SWORDS, resolver.resolve("Weapon_Longsword_Steel"))
    }

    @Test
    fun `resolves daggers to DAGGERS`() {
        assertEquals(SkillType.DAGGERS, resolver.resolve("Weapon_Daggers_Bronze"))
    }

    @Test
    fun `resolves axe to AXES`() {
        assertEquals(SkillType.AXES, resolver.resolve("Weapon_Axe_Iron"))
    }

    @Test
    fun `resolves battleaxe to AXES`() {
        assertEquals(SkillType.AXES, resolver.resolve("Weapon_Battleaxe_Great"))
    }

    @Test
    fun `resolves shortbow to BOWS`() {
        assertEquals(SkillType.BOWS, resolver.resolve("Weapon_Shortbow_Wood"))
    }

    @Test
    fun `resolves crossbow to BOWS`() {
        assertEquals(SkillType.BOWS, resolver.resolve("Weapon_Crossbow_Heavy"))
    }

    @Test
    fun `resolves spear to SPEARS`() {
        assertEquals(SkillType.SPEARS, resolver.resolve("Weapon_Spear_Iron"))
    }

    @Test
    fun `resolves mace to CLUBS`() {
        assertEquals(SkillType.CLUBS, resolver.resolve("Weapon_Mace_Stone"))
    }

    @Test
    fun `resolves club to CLUBS`() {
        assertEquals(SkillType.CLUBS, resolver.resolve("Weapon_Club_Wood"))
    }

    @Test
    fun `returns null for unknown item`() {
        assertNull(resolver.resolve("Tool_Pickaxe_Iron"))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(resolver.resolve(""))
    }
}
