package org.zunkree.hytale.plugins.skillsplugin.skill

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec

data class SkillData(
    var level: Int = 0,
    var totalXP: Double = 0.0,
) {
    companion object {
        val CODEC: BuilderCodec<SkillData> by lazy {
            BuilderCodec
                .builder(
                    SkillData::class.java,
                    ::SkillData,
                ).append(KeyedCodec("Level", Codec.INTEGER), { obj, value -> obj.level = value }, { it.level })
                .add()
                .append(KeyedCodec("TotalXP", Codec.DOUBLE), { obj, value -> obj.totalXP = value }, { it.totalXP })
                .add()
                .build()
        }
    }
}
