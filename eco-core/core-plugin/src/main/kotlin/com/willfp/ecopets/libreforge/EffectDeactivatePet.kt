package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.pets.activePet
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

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
