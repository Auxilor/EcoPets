package com.willfp.ecopets.api.event

import com.willfp.ecopets.pets.Pet
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import javax.annotation.Nullable

class PlayerPetSwapEvent(
    override val player: OfflinePlayer,
    private val pet: Pet?,
    private val newPet: Pet?,
) : PlayerEvent(player as Player), com.willfp.ecopets.api.event.PlayerEvemt {

    fun getOldPet(): Pet? {
        if(pet == null)
            return null
        return pet
    }
    fun getNewPet(): Pet? {
        if(newPet == null)
            return null
        return newPet
    }


    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}