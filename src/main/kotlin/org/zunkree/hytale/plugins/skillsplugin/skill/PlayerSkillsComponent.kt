package org.zunkree.hytale.plugins.skillsplugin.skill

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class PlayerSkillsComponent : Component<EntityStore> {
    val skills: MutableMap<SkillType, SkillData> =
        SkillType.entries.associateWith { SkillData() }.toMutableMap()
    var deathImmunityUntil: Long = 0L

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, PlayerSkillsComponent>

        val CODEC: BuilderCodec<PlayerSkillsComponent> =
            run {
                val builder = BuilderCodec.builder(PlayerSkillsComponent::class.java, ::PlayerSkillsComponent)
                for (skill in SkillType.entries) {
                    builder
                        .append(
                            KeyedCodec(skill.name, SkillData.CODEC),
                            { obj, value -> obj.skills[skill] = value },
                            { it.skills[skill] ?: SkillData() },
                        ).add()
                }
                builder
                    .append(
                        KeyedCodec("deathImmunityUntil", Codec.LONG),
                        { obj, value -> obj.deathImmunityUntil = value },
                        { it.deathImmunityUntil },
                    ).add()
                builder.build()
            }
    }

    override fun clone(): PlayerSkillsComponent {
        val clone = PlayerSkillsComponent()
        for ((key, value) in skills) {
            clone.skills[key] = value.copy()
        }
        clone.deathImmunityUntil = this.deathImmunityUntil
        return clone
    }

    fun getSkill(type: SkillType): SkillData = skills.getOrPut(type) { SkillData() }

    fun setSkill(
        type: SkillType,
        data: SkillData,
    ) {
        skills[type] = data
    }

    val allSkills: Map<SkillType, SkillData> get() = skills.toMap()

    val totalLevels: Int get() = skills.values.sumOf { it.level }
}
