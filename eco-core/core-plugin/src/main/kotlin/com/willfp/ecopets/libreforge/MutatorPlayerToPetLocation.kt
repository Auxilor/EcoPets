package com.willfp.ecopets.libreforge

import com.willfp.ecopets.pets.PetDisplay
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.mutator.Mutator
import wiki.ecohub.rules.mutator.parameterTransformers
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object MutatorPlayerToPetLocation : Mutator<NoCompileData>("player_to_pet_location") {
    override val description = "Re-maps the location to the position of the player's active pet display entity."

    override val categories = setOf("location")

    override val additionalInfo = listOf(
        "Sets the location to null if the player has no active pet display entity."
    )

    override val parameterTransformers = parameterTransformers {
        TriggerParameter.PLAYER becomes TriggerParameter.LOCATION
    }

    override fun mutate(data: TriggerData, config: Config, compileData: NoCompileData): TriggerData {
        val player = data.player ?: return data

        return data.copy(
            location = PetDisplay.get(player)?.location
        )
    }
}
