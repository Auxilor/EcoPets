package com.willfp.ecopets.pets

import com.willfp.libreforge.counters.Accumulator
import org.bukkit.entity.Player

class PetXPAccumulator(
    private val pet: Pet
) : Accumulator {
    override fun accept(player: Player, count: Double) {
        if (player.activePet != pet) {
            return
        }

        player.givePetExperience(pet, count)
    }
}
