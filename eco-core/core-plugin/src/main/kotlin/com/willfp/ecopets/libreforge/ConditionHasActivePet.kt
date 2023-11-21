package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecopets.api.EcoPetsAPI
import com.willfp.ecopets.pets.Pets
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get
import org.bukkit.entity.Player

object ConditionHasActivePet : Condition<NoCompileData>("has_active_pet") {
    override val arguments = arguments {
        require("pet", "You must specify the pet!")
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val player = dispatcher.get<Player>() ?: return false

        return EcoPetsAPI.instance.getActivePet(player) == Pets.getByID(config.getString("pet").lowercase())
    }
}
