package com.willfp.ecopets.pets

import com.willfp.eco.core.cache.EcoCache
import com.willfp.libreforge.counters.Accumulator
import org.bukkit.entity.Player
import java.time.Duration
import kotlin.math.max

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

    // Take the highest matching permission rather than the first one iterated.
    //
    // effectivePermissions is not ordered, so returning on first match made the multiplier
    // depend on iteration order whenever a player had two of these through different groups -
    // the same player could get 1.5x or 3x across a relog with no config change. An
    // unparseable suffix is skipped rather than treated as 100, which previously turned a
    // typo such as `ecopets.xpmultiplier.abc` into a silent 2x.
    //
    // This matches EcoJobs' getNumericalPermission, which already resolved it this way.
    val prefix = "ecopets.xpmultiplier."
    var highest: Double? = null

    for (permissionAttachmentInfo in this.effectivePermissions) {
        val permission = permissionAttachmentInfo.permission

        if (!permission.startsWith(prefix)) {
            continue
        }

        val found = permission.substring(permission.lastIndexOf(".") + 1).toDoubleOrNull() ?: continue
        highest = max(highest ?: found, found)
    }

    return highest?.let { (it / 100) + 1 } ?: 1.0
}