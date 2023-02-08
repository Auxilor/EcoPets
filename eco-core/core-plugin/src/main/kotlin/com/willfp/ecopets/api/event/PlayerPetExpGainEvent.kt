package com.willfp.ecopets.api.event

import com.willfp.ecopets.pets.Pet
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class PlayerPetExpGainEvent(
    who: Player,
    override val pet: Pet,
    var amount: Double,
    val isMultiply: Boolean
) : PlayerEvent(who), Cancellable, PetEvent {
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
