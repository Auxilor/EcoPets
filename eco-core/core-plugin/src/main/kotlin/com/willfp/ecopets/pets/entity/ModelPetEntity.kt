package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.integrations.model.ModelAnimation
import com.willfp.eco.core.integrations.model.ModelManager
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * A pet drawn by a model from the model plugin [integrationId], from `<prefix>:<id>` with the
 * optional `idle-animation:<name>` and `move-animation:<name>` keys after it.
 */
class ModelPetEntity(
    pet: Pet,
    private val integrationId: String,
    texture: String
) : PetEntity(pet) {
    private val args = texture.trim().split(" ").filter { it.isNotBlank() }

    private val modelID = args.firstOrNull().orEmpty()

    private val idleAnimation = argument("idle-animation")

    private val moveAnimation = argument("move-animation")

    private var playingAnimation: String? = null

    override fun spawn(location: Location): Entity {
        val stand = emptyArmorStandAt(location, pet, isSkull = false)

        ModelManager.attachModel(stand, integrationId, modelID)

        return stand
    }

    override fun tick(entity: Entity, isOwnerMoving: Boolean) {
        val next = if (isOwnerMoving) moveAnimation else idleAnimation
        if (next == playingAnimation) {
            return
        }

        playingAnimation?.let { ModelManager.stopAnimation(entity, null, it, true) }
        next?.let { ModelManager.playAnimation(entity, null, it, LOOP) }
        playingAnimation = next
    }

    private fun argument(key: String): String? =
        args.drop(1)
            .firstOrNull { it.startsWith("$key:", ignoreCase = true) }
            ?.run { substringAfter(":").takeIf { it.isNotBlank() } }

    private companion object {
        val LOOP: ModelAnimation = ModelAnimation.builder().loopMode(ModelAnimation.LoopMode.LOOP).build()
    }
}
