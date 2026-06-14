package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getFormattedString
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

object EffectActivatePet : Effect<NoCompileData>("activate_pet") {
    override val description = "Sets the specified pet as the player's active pet, if they own it and it isn't already active."

    override val categories = setOf("player")

    override val additionalInfo = listOf(
        "Has no effect if the player does not own the pet, or if it is already active."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "pet",
            "You must specify the pet to activate!",
            description = "The ID of the pet to activate.",
            type = ArgType.STRING
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false
        val pet = Pets.getByID(config.getFormattedString("pet", data)) ?: return false

        if (!player.hasPet(pet) || player.activePet == pet) return false

        player.activePet = pet

        return true
    }
}
