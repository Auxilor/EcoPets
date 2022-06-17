package com.willfp.ecopets.pets

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.libreforge.chains.EffectChains

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
        plugin.petsYml.getSubsections("chains").mapNotNull {
            EffectChains.compile(it, "Effect Chains")
        }
        for (set in values()) {
            removePet(set)
        }
        for (petConfig in plugin.petsYml.getSubsections("pets")) {
            addNewPet(Pet(petConfig, plugin))
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