package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.getPetLevel
import com.willfp.libreforge.ArgType
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
    override val description = "Passes when the player's level in the specified pet is at or above the given level."

    override val categories = setOf("player")

    override val arguments = arguments {
        require(
            "pet",
            "You must specify the pet!",
            description = "The ID of the pet to check the level of.",
            type = ArgType.STRING
        )
        require(
            "level",
            "You must specify the level!",
            description = "The minimum pet level required.",
            type = ArgType.EXPRESSION
        )
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


        return player.getPetLevel(
            Pets.getByID(config.getString("pet").lowercase()) ?: return false
        ) >= config.getIntFromExpression("level", player)
    }
}
