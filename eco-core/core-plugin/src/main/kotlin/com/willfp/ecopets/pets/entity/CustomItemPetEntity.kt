package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.Items
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.plugin
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack

class CustomItemPetEntity(
    pet: Pet,
    private val itemLookup: String
) : PetEntity(pet) {
    override fun spawn(location: Location): Entity {
        val item: ItemStack = try {
            Items.lookup(itemLookup).item ?: ItemStack(Material.AIR)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to lookup custom pet item: $itemLookup")
            ItemStack(Material.AIR)
        }

        if (plugin.configYml.getBool("pet-entity.item-display.enabled")) {
            val itemDisplay = location.world!!.spawn(location, ItemDisplay::class.java) {
                it.setItemStack(item)
                it.isCustomNameVisible = true
                @Suppress("DEPRECATION")
                it.customName = pet.name
                it.teleportDuration = plugin.configYml.getInt("pet-entity.item-display.teleport-duration", 3)
                
                val scale = plugin.configYml.getDouble("pet-entity.scale")
                if (scale in 0.0625..16.0) {
                    val transform = it.transformation
                    transform.scale.set(scale, scale, scale)
                    it.transformation = transform
                }
            }
            return itemDisplay
        } else {
            val stand = emptyArmorStandAt(location, pet, isSkull = true)
            @Suppress("UNNECESSARY_SAFE_CALL")
            stand.equipment?.helmet = item
            return stand
        }
    }
}
