package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.Items
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack

class ItemStackPetEntity(
    pet: Pet,
    private val itemStack: String
) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        val itemStack: ItemStack = Items.lookup(itemStack).item

        @Suppress("UNNECESSARY_SAFE_CALL") // Can be null.
        stand.equipment?.helmet = itemStack

        return stand
    }
}
