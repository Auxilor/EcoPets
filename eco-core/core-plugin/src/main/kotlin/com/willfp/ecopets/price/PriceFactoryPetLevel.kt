package com.willfp.ecopets.price

import com.willfp.eco.core.placeholder.context.PlaceholderContext
import com.willfp.eco.core.placeholder.context.PlaceholderContextSupplier
import com.willfp.eco.core.price.Price
import com.willfp.eco.core.price.PriceFactory
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.getPetLevel
import com.willfp.ecopets.pets.setPetLevel
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.math.max

class PriceFactoryPetLevel(
    private val pet: Pet
) : PriceFactory {
    override fun getNames() = listOf("${pet.id}_pet_level")

    override fun create(baseContext: PlaceholderContext, function: PlaceholderContextSupplier<Double>): Price {
        return PricePetLevel(baseContext) { function.get(it) }
    }

    private inner class PricePetLevel(
        private val baseContext: PlaceholderContext,
        private val function: (PlaceholderContext) -> Double
    ) : Price {
        private val multipliers = mutableMapOf<UUID, Double>()

        override fun canAfford(player: Player, multiplier: Double): Boolean {
            return player.getPetLevel(pet) >= getValue(player, multiplier)
        }

        override fun pay(player: Player, multiplier: Double) {
            val newLevel = max(0, player.getPetLevel(pet) - getValue(player, multiplier).toInt())
            player.setPetLevel(pet, newLevel)
        }

        override fun giveTo(player: Player, multiplier: Double) {
            player.setPetLevel(pet, player.getPetLevel(pet) + getValue(player, multiplier).toInt())
        }

        override fun getValue(player: Player, multiplier: Double): Double {
            return function(baseContext.copyWithPlayer(player)) * getMultiplier(player) * multiplier
        }

        override fun getMultiplier(player: Player): Double {
            return multipliers[player.uniqueId] ?: 1.0
        }

        override fun setMultiplier(player: Player, multiplier: Double) {
            multipliers[player.uniqueId] = multiplier
        }

        override fun getIdentifier(): String {
            return "ecopets:${pet.id}_pet_level"
        }
    }
}
