package org.zunkree.hytale.plugins.skillsplugin

import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class SkillsComponent : Component<EntityStore> {
    val skills: MutableMap<String, SkillsData> =
        SkillsType.entries.associate { it.name to SkillsData() }.toMutableMap()
    var deathImmunityUntil: Long = 0L

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, SkillsComponent>

        val CODEC: BuilderCodec<SkillsComponent> = run {
            val builder = BuilderCodec.builder(SkillsComponent::class.java, ::SkillsComponent)
            for (skill in SkillsType.entries) {
                builder.append(
                    KeyedCodec(skill.displayName, SkillsData.CODEC),
                    { obj, value -> obj.skills[skill.name] = value },
                    { it.skills[skill.name] ?: SkillsData() }).add()
            }
            builder.build()
        }
    }

    override fun clone(): SkillsComponent {
        val clone = SkillsComponent()
        for ((key, value) in skills) {
            clone.skills[key] = value.copy()
        }
        clone.deathImmunityUntil = this.deathImmunityUntil
        return clone
    }

    fun getSkill(type: SkillsType): SkillsData {
        return skills.getOrPut(type.name) { SkillsData() }
    }

    fun setSkill(type: SkillsType, data: SkillsData) {
        skills[type.name] = data
    }

    val allSkills: Map<String, SkillsData> get() = skills.toMap()

    val totalLevels: Int get() = skills.values.sumOf { it.level }
}
