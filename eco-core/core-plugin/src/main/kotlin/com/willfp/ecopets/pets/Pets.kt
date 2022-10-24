package com.willfp.ecopets.pets

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.readConfig
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecopets.EcoPetsPlugin
import java.io.File

object Pets {
    private val BY_ID: BiMap<String, Pet> = HashBiMap.create()

    /**
     * Get all registered [Pet]s.
     *
     * @return A list of all [Pet]s.
     */
    @JvmStatic
    fun values(): List<Pet> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Get [Pet] matching ID.
     *
     * @param name The name to search for.
     * @return The matching [Pet], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): Pet? {
        return BY_ID[name]
    }

    /**
     * Update all [Pet]s.
     *
     * @param plugin Instance of EcoPets.
     */
    @ConfigUpdater
    @JvmStatic
    fun update(plugin: EcoPetsPlugin) {
        for (set in values()) {
            removePet(set)
        }

        val petsYml = File(plugin.dataFolder, "pets.yml").readConfig(ConfigType.YAML)

        for ((id, petConfig) in plugin.fetchConfigs("pets")) {
            addNewPet(Pet(id, petConfig, plugin))
        }

        for (petConfig in petsYml.getSubsections("pets")) {
            addNewPet(Pet(petConfig.getString("id"), petConfig, plugin))
        }
    }

    /**
     * Add new [Pet] to EcoPets.
     *
     * @param pet The [Pet] to add.
     */
    @JvmStatic
    fun addNewPet(pet: Pet) {
        BY_ID.remove(pet.id)
        BY_ID[pet.id] = pet
    }

    /**
     * Remove [Pet] from EcoPets.
     *
     * @param pet The [Pet] to remove.
     */
    @JvmStatic
    fun removePet(pet: Pet) {
        BY_ID.remove(pet.id)
    }
}