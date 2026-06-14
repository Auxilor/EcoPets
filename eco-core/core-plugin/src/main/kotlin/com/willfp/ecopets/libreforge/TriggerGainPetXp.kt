package com.willfp.ecopets.libreforge

import com.willfp.ecopets.api.event.PlayerPetExpGainEvent
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler

object TriggerGainPetXp : Trigger("gain_pet_xp") {
    override val description = "Fires when the player gains experience towards one of their pets."

    override val categories = setOf("player")

    override val parameterDescriptions = mapOf(
        TriggerParameter.VALUE to "The amount of pet experience gained."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.EVENT,
        TriggerParameter.VALUE
    )

    @EventHandler(ignoreCancelled = true)
    fun handle(event: PlayerPetExpGainEvent) {
        val player = event.player

        this.dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                location = player.location,
                event = event,
                value = event.amount
            )
        )
    }
}
