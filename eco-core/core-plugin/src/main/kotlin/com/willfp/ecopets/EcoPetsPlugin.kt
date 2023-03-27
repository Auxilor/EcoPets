package com.willfp.ecopets

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.ecopets.commands.CommandEcoPets
import com.willfp.ecopets.commands.CommandPets
import com.willfp.ecopets.pets.DiscoverRecipeListener
import com.willfp.ecopets.pets.PetDisplay
import com.willfp.ecopets.pets.PetLevelListener
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.SpawnEggHandler
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.activePetLevel
import com.willfp.ecopets.pets.entity.ModelEnginePetEntity
import com.willfp.ecopets.pets.entity.PetEntity
import com.willfp.libreforge.SimpleProvidedHolder
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.registerHolderProvider
import org.bukkit.event.Listener

class EcoPetsPlugin : LibreforgePlugin() {
    private val petDisplay = PetDisplay(this)

    init {
        instance = this
        registerHolderProvider {
            it.activePetLevel?.let { l ->
                listOf(SimpleProvidedHolder(l))
            } ?: emptyList()
        }
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            Pets
        )
    }

    override fun handleEnable() {
        PlayerPlaceholder(
            this,
            "pet"
        ) { it.activePet?.name ?: "" }.register()

        PlayerPlaceholder(
            this,
            "pet_id"
        ) { it.activePet?.id ?: "" }.register()
    }

    override fun handleReload() {
        if (!this.configYml.getBool("pet-entity.enabled")) {
            return
        }

        this.scheduler.runTimer(1, 1) {
            petDisplay.tickAll()
        }
    }

    override fun handleDisable() {
        petDisplay.shutdown()
    }

    override fun loadIntegrationLoaders(): List<IntegrationLoader> {
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
            CommandEcoPets(this),
            CommandPets(this)
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            PetLevelListener(this),
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
