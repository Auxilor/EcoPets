package com.willfp.ecopets.api.event

import org.bukkit.entity.Player
import com.willfp.ecopets.pets.Pet
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.HandlerList
import com.willfp.ecopets.api.event.PlayerPetExpGainEvent
import org.bukkit.event.Cancellable

class PlayerPetExpGainEvent(
    who: Player,
    val pet: Pet,
    var amount: Double,
    val isMultiply: Boolean
) : PlayerEvent(who), Cancellable {
    private var cancelled = false

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
