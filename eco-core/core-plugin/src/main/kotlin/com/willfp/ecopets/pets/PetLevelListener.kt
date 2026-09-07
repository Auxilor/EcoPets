package com.willfp.ecopets.pets

import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.eco.util.SoundUtils
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.plugin
import com.willfp.ecopets.libreforge.TriggerLevelUpPet
import com.willfp.libreforge.levels.LevelUpDispatcher
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object PetLevelListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onLevelUp(event: PlayerPetLevelUpEvent) {
        val pet = event.pet
        val player = event.player
        val level = event.level

        // Routed through the shared dispatcher so the chain gets %level%, %level_numeral%,
        // %previous_level% and %previous_level_numeral%. Triggering it with a bare Dispatcher,
        // as this did before, attaches no placeholders at all - the level was available here
        // the whole time and simply never passed on.
        //
        // dispatchTrigger = false: TriggerLevelUpPet is its own listener on this same event
        // and already dispatches globally, so dispatching here too would fire every
        // level_up_pet effect twice.
        LevelUpDispatcher.dispatch(
            player.toDispatcher(),
            TriggerLevelUpPet,
            pet.levelUpEffects,
            level,
            TriggerData(
                player = player,
                location = player.location,
                event = event,
                value = level.toDouble()
            )
        )

        PlayableSound.create(plugin.configYml.getSubsection("level-up.sound"))?.playTo(player)

        if (plugin.configYml.getBool("level-up.message.enabled")) {
            for (message in pet.injectPlaceholdersInto(
                plugin.configYml.getFormattedStrings("level-up.message.message"),
                player,
                forceLevel = level
            )) {
                player.sendMessage(message)
            }
        }
    }
}
