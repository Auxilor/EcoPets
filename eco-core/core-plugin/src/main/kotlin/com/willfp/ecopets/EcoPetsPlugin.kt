package com.willfp.ecopets

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.util.toSingletonList
import com.willfp.ecopets.commands.CommandEcopets
import com.willfp.ecopets.commands.CommandPets
import com.willfp.ecopets.pets.DiscoverRecipeListener
import com.willfp.ecopets.pets.PetDisplay
import com.willfp.ecopets.pets.PetLevelListener
import com.willfp.ecopets.pets.PetTriggerXPGainListener
import com.willfp.ecopets.pets.SpawnEggHandler
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.activePetLevel
import com.willfp.ecopets.pets.entity.ModelEnginePetEntity
import com.willfp.ecopets.pets.entity.PetEntity
import com.willfp.libreforge.LibReforgePlugin
import org.bukkit.event.Listener

class EcoPetsPlugin : LibReforgePlugin() {
    private val petDisplay = PetDisplay(this)

    init {
        instance = this
        registerHolderProvider { it.activePetLevel?.toSingletonList() ?: emptyList() }
    }

    override fun handleEnableAdditional() {
        this.copyConfigs("pets")

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
        if (!this.configYml.getBool("pet-entity.enabled")) {
            return
        }

        this.scheduler.runTimer(1, 1) {
            petDisplay.tickAll()
        }
    }

    override fun handleDisableAdditional() {
        petDisplay.shutdown()
    }

    override fun loadAdditionalIntegrations(): List<IntegrationLoader> {
        return listOf(
            IntegrationLoader("ModelEngine") {
                PetEntity.registerPetEntity("modelengine") { pet, id ->
                    ModelEnginePetEntity(pet, id, this)
                }
            }
        )
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
            petDisplay,
            DiscoverRecipeListener(this)
        )
    }

    companion object {
        @JvmStatic
        lateinit var instance: EcoPetsPlugin
    }
}

