package com.willfp.ecopets.api.event

import com.willfp.ecopets.pets.Pet
import org.bukkit.OfflinePlayer
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerPetActivateEvent(
    val who: OfflinePlayer,
    override val pet: Pet
) : Event(), Cancellable, PetEvent {
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
