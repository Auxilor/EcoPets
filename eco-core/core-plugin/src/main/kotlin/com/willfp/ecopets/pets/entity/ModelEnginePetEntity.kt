package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import com.willfp.modelenginebridge.ModelEngineBridge
import org.bukkit.Location
import org.bukkit.entity.Entity

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
    private val plugin: EcoPetsPlugin
) : PetEntity(pet) {
    override fun spawn(location: Location): Entity {
        val stand = emptyArmorStandAt(location, pet, isSkull = false)

        val model = ModelEngineBridge.instance.createActiveModel(modelID) ?: return stand

        val modelled = ModelEngineBridge.instance.createModeledEntity(stand)
        modelled.addModel(model)

        return stand
    }
}
