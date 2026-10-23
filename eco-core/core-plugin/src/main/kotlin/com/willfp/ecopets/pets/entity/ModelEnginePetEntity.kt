package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.integrations.modelengine.ModelEngineManager
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * A pet drawn by a ModelEngine model, from `modelengine:<id>` with the optional
 * `idle-animation:<name>` and `move-animation:<name>` keys after it.
 */
class ModelEnginePetEntity(
    pet: Pet,
    texture: String
) : PetEntity(pet) {
    private val args = texture.trim().split(" ").filter { it.isNotBlank() }

    private val modelID = args.firstOrNull().orEmpty()

    private val idleAnimation = argument("idle-animation")

    private val moveAnimation = argument("move-animation")

    private var playingAnimation: String? = null

    override fun spawn(location: Location): Entity {
        val stand = emptyArmorStandAt(location, pet, isSkull = false)

        val model = ModelEngineManager.createActiveModel(modelID) ?: return stand

        val modelled = ModelEngineManager.createModeledEntity(stand) ?: return stand
        modelled.addModel(model)

        return stand
    }

    override fun tick(entity: Entity, isOwnerMoving: Boolean) {
        val next = if (isOwnerMoving) moveAnimation else idleAnimation
        if (next == playingAnimation) {
            return
        }

        for (model in ModelEngineManager.getModeledEntity(entity)?.models?.values.orEmpty()) {
            val handler = model.animationHandler
            playingAnimation?.let(handler::stopAnimation)
            next?.let { handler.playAnimation(it, 0.0, 0.0, 1.0, false, true) }
        }
        playingAnimation = next
    }

    private fun argument(key: String): String? =
        args.drop(1)
            .firstOrNull { it.startsWith("$key:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.takeIf { it.isNotBlank() }
}
