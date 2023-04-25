package com.willfp.ecopets

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.ecopets.commands.CommandEcoPets
import com.willfp.ecopets.commands.CommandPets
import com.willfp.ecopets.libreforge.ConditionHasActivePet
import com.willfp.ecopets.libreforge.ConditionHasPetLevel
import com.willfp.ecopets.libreforge.EffectGivePetXp
import com.willfp.ecopets.libreforge.EffectPetXpMultiplier
import com.willfp.ecopets.libreforge.FilterPet
import com.willfp.ecopets.libreforge.TriggerGainPetXp
import com.willfp.ecopets.libreforge.TriggerLevelUpPet
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
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.registerHolderProvider
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.event.Listener

class EcoPetsPlugin : LibreforgePlugin() {
    private val petDisplay = PetDisplay(this)

    init {
        instance = this
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            Pets
        )
    }

    override fun handleEnable() {
        Conditions.register(ConditionHasPetLevel)
        Conditions.register(ConditionHasActivePet)
        Effects.register(EffectPetXpMultiplier)
        Effects.register(EffectGivePetXp)
        Triggers.register(TriggerGainPetXp)
        Triggers.register(TriggerLevelUpPet)
        Filters.register(FilterPet)

        registerHolderProvider {
            it.activePetLevel?.let { l ->
                listOf(SimpleProvidedHolder(l))
            } ?: emptyList()
        }

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

        this.scheduler.runTimerAsync(1, 1) { petDisplay.tickAll() }
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
