package com.willfp.ecopets.pets

import com.willfp.eco.core.cache.EcoCache
import com.willfp.libreforge.counters.Accumulator
import org.bukkit.entity.Player
import java.time.Duration
import com.willfp.eco.util.NumericalPermissions

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

private val expMultiplierCache = EcoCache.builder<Player, Double>()
    .expireAfterWrite(Duration.ofSeconds(10))
    .build {
        it.cachePetExperienceMultiplier()
    }

val Player.petExperienceMultiplier: Double
    get() = expMultiplierCache.get(this) { it.cachePetExperienceMultiplier() }

private fun Player.cachePetExperienceMultiplier(): Double {
    if (this.hasPermission("ecopets.xpmultiplier.quadruple")) {
        return 4.0
    }

    if (this.hasPermission("ecopets.xpmultiplier.triple")) {
        return 3.0
    }

    if (this.hasPermission("ecopets.xpmultiplier.double")) {
        return 2.0
    }

    if (this.hasPermission("ecopets.xpmultiplier.50percent")) {
        return 1.5
    }

    // Highest matching permission, not the first one iterated: effectivePermissions is
    // unordered, so a player holding two of these through different groups used to get a
    // different multiplier across a relog with no config change. Shared with the other
    // plugins via eco so the four cannot drift apart again.
    return 1 + NumericalPermissions.highest(
        this.effectivePermissions.filter { it.value }.map { it.permission },
        "ecopets.xpmultiplier",
        0.0
    ) / 100
}