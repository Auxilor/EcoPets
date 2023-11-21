package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.api.EcoPetsAPI
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.pets.Pets
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.updateEffects
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

object ConditionHasPetLevel : Condition<NoCompileData>("has_pet_level") {
    override val arguments = arguments {
        require("pet", "You must specify the pet!")
        require("level", "You must specify the level!")
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handle(event: PlayerPetLevelUpEvent) {
        event.player.toDispatcher().updateEffects()
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val player = dispatcher.get<Player>() ?: return false

        return EcoPetsAPI.instance.getPetLevel(
            player,
            Pets.getByID(config.getString("pet").lowercase()) ?: return false
        ) >= config.getIntFromExpression("level", player)
    }
}
