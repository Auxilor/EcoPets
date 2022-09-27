package com.willfp.ecopets.pets.entity

import com.ticxo.modelengine.api.ModelEngineAPI
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)
        val entityAnimation = pet.entityAnimation;

        val model = ModelEngineAPI.createActiveModel(modelID)
        val animationHandler = model.animationHandler;
        val animationProperty = animationHandler.getAnimation(entityAnimation);

        if (animationProperty != null) {
            animationHandler.playAnimation(animationProperty, true)
        } else {
            EcoPetsPlugin.instance.logger.warning("$entityAnimation not found in model $modelID, im use walk animation")
            val animationPropertyWalk = animationHandler.getAnimation("walk");
            if (animationPropertyWalk != null) {
                animationHandler.playAnimation(animationPropertyWalk, true)
            } else {
                EcoPetsPlugin.instance.logger.warning("walk animation not found in $modelID, you have any animation!?")
            }
        }

        val modelled = ModelEngineAPI.createModeledEntity(stand)
        modelled.addModel(model, true)

        return stand
    }
}
