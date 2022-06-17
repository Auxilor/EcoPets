package com.willfp.ecopets.api.event

import org.bukkit.entity.Player
import com.willfp.ecopets.pets.Pet
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.HandlerList
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent

class PlayerPetLevelUpEvent(
    who: Player,
    val pet: Pet,
    val level: Int
) : PlayerEvent(who) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
