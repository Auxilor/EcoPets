package com.willfp.ecopets.libreforge

import com.willfp.ecopets.api.event.PlayerPetExpGainEvent
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.Pets
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.templates.MultiMultiplierEffect
import com.willfp.libreforge.toDispatcher
import org.bukkit.event.EventHandler

object EffectPetXpMultiplier : MultiMultiplierEffect<Pet>("pet_xp_multiplier") {
    override val description = "Multiplies XP gained for one or all EcoPets pets while the holder is active."

    override val categories = setOf("player")

    override val arguments = arguments {
        require(
            "multiplier",
            "You must specify the multiplier!",
            description = "The XP multiplier. Supports expressions.",
            type = ArgType.EXPRESSION
        )
        optional(
            "pets",
            description = "List of pet names to apply the multiplier to. If omitted, applies to all pets.",
            type = ArgType.STRING_LIST
        )
    }

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
