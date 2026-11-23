package com.willfp.ecopets.libreforge

import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.hasPet
import org.bukkit.entity.Player
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get

object ConditionHasPet : Condition<NoCompileData>("has_pet") {
    override val description = "Passes when the player owns the specified pet, whether or not it is currently active."

    override val categories = setOf("player")

    override val arguments = arguments {
        require(
            "pet",
            "You must specify the pet!",
            description = "The ID of the pet the player must own.",
            type = ArgType.STRING
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val player = dispatcher.get<Player>() ?: return false

        return player.hasPet(
            Pets.getByID(config.getString("pet").lowercase()) ?: return false
        )
    }
}