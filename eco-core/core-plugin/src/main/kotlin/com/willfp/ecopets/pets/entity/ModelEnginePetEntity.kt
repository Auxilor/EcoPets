package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import com.willfp.modelenginebridge.ModelEngineBridge
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
    private val plugin: EcoPetsPlugin
) : PetEntity(pet) {

    val animation = pet.config.getStringOrNull("modelengine-animation")

    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        val modelled = ModelEngineBridge.instance.createModeledEntity(stand)

        val model = ModelEngineBridge.instance.createActiveModel(modelID) ?: return stand

        if (animation != null) {
            model.animationHandler.playAnimation(animation, 0.3, 0.3, 1.0, true)
        }

        modelled.addModel(model)

        return stand
    }
}
