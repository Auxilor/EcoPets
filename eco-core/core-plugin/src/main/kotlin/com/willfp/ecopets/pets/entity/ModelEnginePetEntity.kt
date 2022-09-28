package com.willfp.ecopets.pets.entity

import com.ticxo.modelengine.api.ModelEngineAPI
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
    private val plugin: EcoPetsPlugin
) : PetEntity(pet) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)
        val meAnimation = pet.modelEngineAnimation

        val model = ModelEngineAPI.createActiveModel(modelID)

        if (meAnimation != null) {
            val animationHandler = model.animationHandler
            val animationProperty = animationHandler.getAnimation(meAnimation)

            if (animationProperty != null) {
                animationHandler.playAnimation(animationProperty, true)
            } else {
                plugin.logger.warning("Animation $meAnimation not found in model $modelID, defaulting to walk!")
                val animationPropertyWalk = animationHandler.getAnimation("walk")
                if (animationPropertyWalk != null) {
                    animationHandler.playAnimation(animationPropertyWalk, true)
                } else {
                    plugin.logger.warning("Walk animation not found in $modelID!")
                }
            }
        }

        val modelled = ModelEngineAPI.createModeledEntity(stand)
        modelled.addModel(model, true)

        return stand
    }
}
