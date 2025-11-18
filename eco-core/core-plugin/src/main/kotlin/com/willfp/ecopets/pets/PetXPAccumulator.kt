package com.willfp.ecopets.pets

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.libreforge.counters.Accumulator
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

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

private val expMultiplierCache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.SECONDS)
    .build<Player, Double> {
        it.cachePetExperienceMultiplier()
    }

val Player.petExperienceMultiplier: Double
    get() = expMultiplierCache.get(this)!!

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

    val prefix = "ecopets.xpmultiplier."
    for (permissionAttachmentInfo in this.effectivePermissions) {
        val permission = permissionAttachmentInfo.permission
        if (permission.startsWith(prefix)) {
            return ((permission.substring(permission.lastIndexOf(".") + 1).toDoubleOrNull() ?: 100.0) / 100) + 1
        }
    }

    return 1.0
}