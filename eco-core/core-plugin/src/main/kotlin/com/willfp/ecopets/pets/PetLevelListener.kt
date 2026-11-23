package com.willfp.ecopets.pets

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.libreforge.TriggerLevelUpPet
import com.willfp.ecopets.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.progression.LevelUpDispatcher
import wiki.ecohub.rules.trigger.TriggerData

object PetLevelListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onLevelUp(event: PlayerPetLevelUpEvent) {
        val pet = event.pet
        val player = event.player
        val level = event.level

        // Routed through the shared dispatcher so the chain gets %level_numeral%,
        // %previous_level% and %previous_level_numeral% alongside %level%.
        //
        // %level% already resolved here before this change, but from a different source: the
        // element's config has a PlayerStaticPlaceholder("level") injected into it, and the
        // chain is compiled from that same config, so %level% read getPetLevel -
        // the holder's *current* level rather than the level this particular event is for.
        // The two agree for an ordinary single-level gain, because the level is committed
        // before the events fire. They disagree on a multi-level grant, where every event
        // would read the final level and a `require: "%level% = 5"` reward could be skipped.
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
            ),
            type = "level"
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
