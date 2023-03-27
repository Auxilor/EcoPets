package com.willfp.ecopets.pets

import com.willfp.libreforge.counters.Accumulator
import org.bukkit.entity.Player

class PetXPAccumulator(
    private val pet: Pet
) : Accumulator {
    override fun accept(player: Player, count: Double) {
        player.givePetExperience(pet, count)
    }
}
