package org.zunkree.hytale.plugins.skillsplugin.resolver

import org.junit.jupiter.api.Test
import org.zunkree.hytale.plugins.skillsplugin.skill.SkillType
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockSkillResolverTest {
    private val resolver = BlockSkillResolver()

    @Test
    fun `resolves Rock blocks to MINING`() {
        assertEquals(SkillType.MINING, resolver.resolve("Rock_Granite"))
    }

    @Test
    fun `resolves Ore blocks to MINING`() {
        assertEquals(SkillType.MINING, resolver.resolve("Ore_Iron"))
    }

    @Test
    fun `resolves Wood blocks to WOODCUTTING`() {
        assertEquals(SkillType.WOODCUTTING, resolver.resolve("Wood_Oak"))
    }

    @Test
    fun `returns null for unknown block type`() {
        assertNull(resolver.resolve("Grass_Plains"))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(resolver.resolve(""))
    }

    @Test
    fun `is case sensitive`() {
        assertNull(resolver.resolve("rock_granite"))
    }
}
