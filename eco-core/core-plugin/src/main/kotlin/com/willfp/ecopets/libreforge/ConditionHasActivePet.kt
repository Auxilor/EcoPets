package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get
import org.bukkit.entity.Player

object ConditionHasActivePet : Condition<NoCompileData>("has_active_pet") {
    override val description = "Passes when the player's currently active pet is the specified pet."

    override val categories = setOf("player")

    override val arguments = arguments {
        require(
            "pet",
            "You must specify the pet!",
            description = "The ID of the pet that must be the player's active pet.",
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

        return player.activePet == Pets.getByID(config.getString("pet").lowercase())
    }
}
