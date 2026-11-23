package com.willfp.ecopets.libreforge

import com.willfp.ecopets.pets.PetDisplay
import org.bukkit.entity.LivingEntity
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.mutator.Mutator
import wiki.ecohub.rules.mutator.parameterTransformers
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object MutatorPlayerToPet : Mutator<NoCompileData>("player_to_pet") {
    override val description = "Re-maps the victim to the player's active pet display entity."

    override val categories = setOf("victim")

    override val additionalInfo = listOf(
        "Sets the victim to null if the player has no active pet display entity."
    )

    override val parameterTransformers = parameterTransformers {
        TriggerParameter.PLAYER becomes TriggerParameter.VICTIM
    }

    override fun mutate(data: TriggerData, config: Config, compileData: NoCompileData): TriggerData {
        val player = data.player ?: return data

        return data.copy(
            victim = PetDisplay.get(player) as? LivingEntity
        )
    }
}
