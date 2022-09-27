package com.willfp.ecopets.pets.entity

import com.ticxo.modelengine.api.ModelEngineAPI
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
    private val animationString: String,
) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        val model = ModelEngineAPI.createActiveModel(modelID)
        val animationHandler = model.animationHandler;
        val modelAnimation = animationHandler.getAnimation(animationString);

        if (modelAnimation != null) animationHandler.playAnimation(modelAnimation, true);

        val modelled = ModelEngineAPI.createModeledEntity(stand)
        modelled.addModel(model, true)

        return stand
    }
}
