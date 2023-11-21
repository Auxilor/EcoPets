package com.willfp.ecopets.libreforge

import com.willfp.ecopets.api.event.PlayerPetExpGainEvent
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.Pets
import com.willfp.libreforge.effects.templates.MultiMultiplierEffect
import com.willfp.libreforge.toDispatcher
import org.bukkit.event.EventHandler

object EffectPetXpMultiplier : MultiMultiplierEffect<Pet>("pet_xp_multiplier") {
    override val key = "pets"

    override fun getElement(key: String): Pet? {
        return Pets.getByID(key.lowercase())
    }

    override fun getAllElements(): Collection<Pet> {
        return Pets.values()
    }

    @EventHandler(ignoreCancelled = true)
    fun handle(event: PlayerPetExpGainEvent) {
        val player = event.player

        event.amount *= getMultiplier(player.toDispatcher(), event.pet)
    }
}
