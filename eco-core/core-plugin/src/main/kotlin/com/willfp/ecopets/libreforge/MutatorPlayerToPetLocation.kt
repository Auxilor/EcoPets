package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.pets.PetDisplay
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.mutators.Mutator
import com.willfp.libreforge.mutators.parameterTransformers
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

object MutatorPlayerToPetLocation : Mutator<NoCompileData>("player_to_pet_location") {
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
