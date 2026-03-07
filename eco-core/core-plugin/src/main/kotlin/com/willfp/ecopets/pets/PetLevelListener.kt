package com.willfp.ecopets.pets

import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.libreforge.toDispatcher
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class PetLevelListener(
    private val plugin: EcoPetsPlugin
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onLevelUp(event: PlayerPetLevelUpEvent) {
        val pet = event.pet
        val player = event.player
        val level = event.level

        pet.levelUpEffects?.trigger(player.toDispatcher())
        pet.executeLevelCommands(player, level)

        PlayableSound.create(plugin.configYml.getSubsection("level-up.sound"))?.playTo(player)

        if (this.plugin.configYml.getBool("level-up.message.enabled")) {
            for (message in pet.injectPlaceholdersInto(
                this.plugin.configYml.getFormattedStrings("level-up.message.message"),
                player,
                forceLevel = level
            )) {
                player.sendMessage(message)
            }
        }
    }
}
