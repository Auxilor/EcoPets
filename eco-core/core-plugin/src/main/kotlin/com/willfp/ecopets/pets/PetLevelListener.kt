package com.willfp.ecopets.pets

import com.willfp.eco.util.SoundUtils
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.libreforge.toDispatcher
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

        pet.levelUpEffects?.trigger(player.toDispatcher())
        pet.executeLevelCommands(player, level)

        if (this.plugin.configYml.getBool("level-up.sound.enabled")) {
            val sound = SoundUtils.getSound(this.plugin.configYml.getString("level-up.sound.id"))
            val pitch = this.plugin.configYml.getDouble("level-up.sound.pitch")

            if (sound != null) {
                player.playSound(
                    player.location,
                    sound,
                    100f,
                    pitch.toFloat()
                )
            }
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
