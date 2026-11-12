package com.willfp.ecopets.libreforge

import com.willfp.ecopets.pets.activePet
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object EffectDeactivatePet : Effect<NoCompileData>("deactivate_pet") {
    override val description = "Removes the player's currently active pet, if they have one active."

    override val categories = setOf("player")

    override val additionalInfo = listOf(
        "Has no effect if the player has no active pet."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        if (player.activePet == null) return false

        player.activePet = null

        return true
    }
}
