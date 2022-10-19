package com.willfp.ecopets.pets

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.conditions.ConfiguredCondition
import com.willfp.libreforge.events.TriggerPreProcessEvent
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.triggers.Trigger
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

data class PetXPGain(
    val trigger: Trigger,
    val multiplier: Double,
    val conditions: Iterable<ConfiguredCondition>,
    val filters: Config
)

object PetTriggerXPGainListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun handle(event: TriggerPreProcessEvent) {
        val player = event.player
        val trigger = event.trigger
        val value = event.value
        val data = event.data

        val pet = event.player.activePet ?: return

        val xpGain = pet.getPetXPGain(trigger) ?: return

        if (xpGain.conditions.any { !it.isMet(player) }) {
            return
        }

        if (!Filters.passes(data, xpGain.filters)) {
            return
        }

        player.givePetExperience(
            pet,
            value * xpGain.multiplier
        )
    }
}
