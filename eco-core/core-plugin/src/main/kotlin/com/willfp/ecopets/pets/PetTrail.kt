package com.willfp.ecopets.pets

import org.bukkit.Location
import wiki.ecohub.config.Config
import wiki.ecohub.game.particle.Particles
import wiki.ecohub.game.particle.SpawnableParticle

/**
 * A particle trail left behind by a pet as it follows its owner.
 */
class PetTrail(
    private val particle: SpawnableParticle,
    private val gap: Int,
    private val amount: Int,
    private val yOffset: Double
) {
    fun shouldSpawnOn(tick: Long) = tick % gap == 0L

    fun spawn(location: Location, anchorY: Double) {
        particle.spawn(location.clone().add(0.0, anchorY + yOffset, 0.0), amount)
    }

    companion object {
        fun fromConfig(config: Config): PetTrail? {
            if (!config.getBool("enabled")) {
                return null
            }

            return PetTrail(
                Particles.lookup(config.getString("particle")),
                (config.getIntOrNull("gap") ?: 2).coerceAtLeast(1),
                (config.getIntOrNull("amount") ?: 1).coerceAtLeast(1),
                config.getDoubleOrNull("y-offset") ?: 0.0
            )
        }
    }
}
