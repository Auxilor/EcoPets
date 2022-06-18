package com.willfp.ecopets

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.util.toSingletonList
import com.willfp.ecopets.commands.CommandEcopets
import com.willfp.ecopets.commands.CommandPets
import com.willfp.ecopets.config.PetsYml
import com.willfp.ecopets.pets.PetDisplay
import com.willfp.ecopets.pets.PetLevelListener
import com.willfp.ecopets.pets.PetTriggerXPGainListener
import com.willfp.ecopets.pets.SpawnEggHandler
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.activePetLevel
import com.willfp.libreforge.LibReforgePlugin
import org.bukkit.event.Listener

class EcoPetsPlugin : LibReforgePlugin() {
    val petsYml: PetsYml

    private val petDisplay = PetDisplay(this)

    init {
        instance = this
        petsYml = PetsYml(this)
        registerHolderProvider { it.activePetLevel?.toSingletonList() ?: emptyList() }
    }

    override fun handleEnableAdditional() {
        PlayerPlaceholder(
            this,
            "pet"
        ) { it.activePet?.name ?: "" }.register()

        PlayerPlaceholder(
            this,
            "pet_id"
        ) { it.activePet?.id ?: "" }.register()
    }

    override fun handleReloadAdditional() {
        this.scheduler.runTimer(1, 1) {
            petDisplay.tickAll()
        }
    }

    override fun handleDisableAdditional() {
        petDisplay.shutdown()
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcopets(this),
            CommandPets(this)
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            PetLevelListener(this),
            PetTriggerXPGainListener,
            SpawnEggHandler(this),
            petDisplay
        )
    }

    override fun getMinimumEcoVersion(): String {
        return "6.37.0"
    }

    companion object {
        @JvmStatic
        lateinit var instance: EcoPetsPlugin
    }
}

