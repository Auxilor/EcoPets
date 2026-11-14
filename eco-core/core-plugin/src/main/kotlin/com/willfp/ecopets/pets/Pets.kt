package com.willfp.ecopets.pets

import com.google.common.collect.ImmutableList
import wiki.ecohub.config.Config
import wiki.ecohub.config.LegacyLocation
import wiki.ecohub.config.category.NativeConfigCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.price.Prices
import wiki.ecohub.core.registry.Registry
import wiki.ecohub.modules.recipe.Recipes

object Pets : NativeConfigCategory("pets", "pet") {
    private val registry = Registry<Pet>()

    override val legacyLocation = LegacyLocation(
        "pets.yml",
        "pets"
    )

    /**
     * Get all registered [Pet]s.
     *
     * @return A list of all [Pet]s.
     */
    @JvmStatic
    fun values(): List<Pet> {
        return ImmutableList.copyOf(registry.values())
    }

    /**
     * Get [Pet] matching ID.
     *
     * @param name The name to search for.
     * @return The matching [Pet], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): Pet? {
        return registry[name]
    }

    override fun clear(plugin: EcoPlugin) {
        for (pet in registry.values()) {
            Prices.unregisterPriceFactory(pet.priceFactory)
            pet.recipe?.let { Recipes.unregister(it) }
        }
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        registry.register(Pet(id, config))
    }
}
