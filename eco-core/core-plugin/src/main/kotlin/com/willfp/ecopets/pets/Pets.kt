package com.willfp.ecopets.pets

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.loader.configs.LegacyLocation

object Pets : ConfigCategory("pet", "pets") {
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

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Pet(id, config, plugin as EcoPetsPlugin))
    }
}
