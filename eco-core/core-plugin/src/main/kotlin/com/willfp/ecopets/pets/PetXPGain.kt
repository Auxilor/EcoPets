package com.willfp.ecopets.pets

import com.willfp.libreforge.conditions.ConfiguredCondition
import com.willfp.libreforge.events.TriggerPreProcessEvent
import com.willfp.libreforge.triggers.Trigger
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

data class PetXPGain(
    val trigger: Trigger,
    val multiplier: Double,
    val conditions: Iterable<ConfiguredCondition>
)

object PetTriggerXPGainListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun handle(event: TriggerPreProcessEvent) {
        val player = event.player
        val trigger = event.trigger
        val value = event.value

        val pet = event.player.activePet ?: return

        val xpGain = pet.getPetXPGain(trigger) ?: return

        if (xpGain.conditions.any { !it.isMet(player) }) {
            return
        }

        player.givePetExperience(
            pet,
            value * xpGain.multiplier
        )
    }
}
