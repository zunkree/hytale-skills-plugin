package org.zunkree.hytale.plugins.skillsplugin

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec

data class SkillsData(
    var level: Int = 0,
    var totalXP: Float = 0f,
) {
    companion object {
        const val MAX_LEVEL = 100
        val CODEC: BuilderCodec<SkillsData> = BuilderCodec.builder(
            SkillsData::class.java,
            ::SkillsData
        )
            .append(KeyedCodec("Level", Codec.INTEGER), {obj, value -> obj.level = value}, {it.level}).add()
            .append(KeyedCodec("TotalXP", Codec.FLOAT), {obj, value -> obj.totalXP = value}, {it.totalXP}).add()
            .build()
    }

    fun getLevelProgress(): Float {
        if (level >= MAX_LEVEL) return 1f

        val xpForNext = SkillsXpCalculator.cumulativeXpForLevel(level + 1)
        val xpForCurrent = SkillsXpCalculator.cumulativeXpForLevel(level)
        val xpIntoLevel = totalXP - xpForCurrent
        val xpNeeded = xpForNext - xpForCurrent
        return (xpIntoLevel / xpNeeded).coerceIn(0f, 1f)
    }
}
