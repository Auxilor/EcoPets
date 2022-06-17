package com.willfp.ecopets.pets

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import org.bukkit.Sound
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

        pet.executeLevelCommands(player, level)

        if (this.plugin.configYml.getBool("level-up.sound.enabled")) {
            val sound = Sound.valueOf(this.plugin.configYml.getString("level-up.sound.id").uppercase())
            val pitch = this.plugin.configYml.getDouble("level-up.sound.pitch")

            player.playSound(
                player.location,
                sound,
                100f,
                pitch.toFloat()
            )
        }

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
