package com.willfp.ecopets.api.event

import com.willfp.ecopets.pets.Pet
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class PlayerPetLevelUpEvent(
    who: Player,
    override val pet: Pet,
    val level: Int
) : PlayerEvent(who), PetEvent {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
