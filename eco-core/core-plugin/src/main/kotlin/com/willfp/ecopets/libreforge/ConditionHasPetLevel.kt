package com.willfp.ecopets.libreforge

import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.getPetLevel
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get
import wiki.ecohub.rules.holder.updateEffects

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
            type = ArgType.EXPRESSION,
            example = "5 + %level%"
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
