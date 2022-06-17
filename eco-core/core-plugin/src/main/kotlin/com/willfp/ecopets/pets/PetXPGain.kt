package com.willfp.ecopets.pets

import com.willfp.libreforge.events.TriggerPreProcessEvent
import com.willfp.libreforge.triggers.Trigger
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

data class PetXPGain(
    val trigger: Trigger,
    val multiplier: Double
)

object PetTriggerXPGainListener: Listener {
    @EventHandler(ignoreCancelled = true)
    fun handle(event: TriggerPreProcessEvent) {
        val player = event.player
        val trigger = event.trigger
        val value = event.value

        val pet = event.player.activePet ?: return

        if (pet.canGainXPFromTrigger(trigger)) {
            player.givePetExperience(
                pet,
                value * pet.getTriggerXPMultiplier(trigger)
            )
        }
    }
}
