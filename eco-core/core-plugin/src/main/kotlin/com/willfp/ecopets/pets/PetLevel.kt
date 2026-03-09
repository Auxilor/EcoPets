package com.willfp.ecopets.pets

import com.willfp.ecopets.plugin
import com.willfp.libreforge.Holder
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.effects.EffectList

class PetLevel(
    val pet: Pet,
    val level: Int,
    override val effects: EffectList,
    override val conditions: ConditionList
) : Holder {
    override val id = plugin.createNamespacedKey("${pet.id}_$level")
}
