package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.builder.SkullBuilder
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.plugin
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack

class ItemDisplayPetEntity(
    pet: Pet
) : PetEntity(pet) {
    override fun spawn(location: Location): Entity {
        val skull: ItemStack = SkullBuilder()
            .setSkullTexture(pet.entityTexture)
            .build()

        val itemDisplay = location.world!!.spawn(location, ItemDisplay::class.java) {
            it.setItemStack(skull)
            it.isCustomNameVisible = true
            @Suppress("DEPRECATION")
            it.customName = pet.name
            it.teleportDuration = plugin.configYml.getInt("pet-entity.item-display.teleport-duration", 3)
        }

        return itemDisplay
    }
}