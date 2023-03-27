package com.willfp.ecopets.pets

import com.willfp.libreforge.counters.Accumulator

class PetXPAccumulator(
    private val pet: Pet
) : Accumulator {
    override fun accept(count: Double) {
        player.givePetExperience(pet, count)
    }
}
