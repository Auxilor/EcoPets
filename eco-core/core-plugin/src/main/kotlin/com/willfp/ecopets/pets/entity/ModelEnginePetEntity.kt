package com.willfp.ecopets.pets.entity

import com.ticxo.modelengine.api.ModelEngineAPI
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String
) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        val model = ModelEngineAPI.createActiveModel(modelID)
        val modelled = ModelEngineAPI.createModeledEntity(stand)
        modelled.addModel(model, true)

        return stand
    }
}
