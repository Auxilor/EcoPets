package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.builder.SkullBuilder
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack

class SkullPetEntity(pet: Pet) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        val skull: ItemStack = SkullBuilder()
            .setSkullTexture(pet.entityTexture)
            .build()

        stand.equipment?.helmet = skull

        return stand
    }
}
