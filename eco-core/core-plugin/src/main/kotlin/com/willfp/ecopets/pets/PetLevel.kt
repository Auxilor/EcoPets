package com.willfp.ecopets.pets

import com.willfp.libreforge.Holder
import com.willfp.libreforge.conditions.ConfiguredCondition
import com.willfp.libreforge.effects.ConfiguredEffect

class PetLevel(
    val pet: Pet,
    val level: Int,
    override val effects: Set<ConfiguredEffect>,
    override val conditions: Set<ConfiguredCondition>
): Holder {
    override val id = "${pet.id}_$level"
}
