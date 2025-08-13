package com.willfp.ecopets.api.event

import com.willfp.ecopets.pets.Pet
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class PlayerAdoptPetEvent(
    override val player: OfflinePlayer,
    private val pet: Pet?,
) : PlayerEvent(player as Player), com.willfp.ecopets.api.event.PlayerEvemt {

    fun getPet(): Pet? {
        return pet;
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }

}