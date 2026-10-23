package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.integrations.modelengine.ModelEngineManager
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.Entity

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String
) : PetEntity(pet) {
    override fun spawn(location: Location): Entity {
        val stand = emptyArmorStandAt(location, pet, isSkull = false)

        val model = ModelEngineManager.createActiveModel(modelID) ?: return stand

        val modelled = ModelEngineManager.createModeledEntity(stand) ?: return stand
        modelled.addModel(model)

        return stand
    }
}
