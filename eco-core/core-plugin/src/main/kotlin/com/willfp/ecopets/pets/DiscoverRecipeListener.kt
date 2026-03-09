package com.willfp.ecopets.pets

import com.willfp.ecopets.plugin
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Recipe

object DiscoverRecipeListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!plugin.configYml.getBool("discover-recipes")) {
            return
        }
        mutableListOf<Recipe>()
            .apply { Bukkit.getServer().recipeIterator().forEachRemaining(this::add) }
            .filterIsInstance<Keyed>().map { it.key }
            .filter { it.namespace == plugin.name.lowercase() }
            .filter { !it.key.contains("displayed") }
            .forEach { event.player.discoverRecipe(it) }
    }
}