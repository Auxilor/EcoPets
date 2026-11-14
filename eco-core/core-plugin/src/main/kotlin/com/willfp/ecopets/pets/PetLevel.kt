package com.willfp.ecopets.pets

import com.willfp.ecopets.plugin
import wiki.ecohub.rules.condition.ConditionList
import wiki.ecohub.rules.effect.EffectList
import wiki.ecohub.rules.holder.Holder

class PetLevel(
    val pet: Pet,
    val level: Int,
    override val effects: EffectList,
    override val conditions: ConditionList
) : Holder {
    override val id = plugin.createNamespacedKey("${pet.id}_$level")
}
