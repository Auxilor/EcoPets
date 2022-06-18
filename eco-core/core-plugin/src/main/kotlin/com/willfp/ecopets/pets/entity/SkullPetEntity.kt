package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.builder.SkullBuilder
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class SkullPetEntity(pet: Pet) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val newStand = location.world!!.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        newStand.isVisible = false
        newStand.isInvulnerable = true
        newStand.isPersistent = true
        newStand.removeWhenFarAway = false
        newStand.isSmall = true
        newStand.setGravity(false)
        newStand.isCollidable = false

        for (slot in EquipmentSlot.values()) {
            newStand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
        }


        val skull: ItemStack = SkullBuilder()
            .setSkullTexture(pet.entityTexture)
            .build()

        newStand.equipment?.helmet = skull
        newStand.isCustomNameVisible = true
        newStand.customName = pet.name

        return newStand
    }
}
