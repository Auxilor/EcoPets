package com.willfp.ecopets.pets.entity

import com.ticxo.modelengine.api.ModelEngineAPI
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot

class ModelEnginePetEntity(
    pet: Pet
) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = location.world!!.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        stand.isVisible = false
        stand.isInvulnerable = true
        stand.isPersistent = true
        stand.removeWhenFarAway = false
        stand.isSmall = true
        stand.setGravity(false)
        stand.isCollidable = false

        for (slot in EquipmentSlot.values()) {
            stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
        }

        val model = ModelEngineAPI.createActiveModel(pet.entityTexture.removePrefix("modelengine:"))
        val modelled = ModelEngineAPI.createModeledEntity(stand)
        modelled.addActiveModel(model)

        stand.isCustomNameVisible = true
        stand.customName = pet.name

        return stand
    }
}
