package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.items.builder.SkullBuilder
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.plugin
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack

// Paper rejects ItemDisplay teleport durations outside 0 to 59 ticks.
private const val DEFAULT_SMOOTHING_TICKS = 3
private const val MIN_SMOOTHING_TICKS = 0
private const val MAX_SMOOTHING_TICKS = 59

class ItemDisplayPetEntity(
    pet: Pet
) : PetEntity(pet) {
    override fun spawn(location: Location): Entity {
        val skull: ItemStack = SkullBuilder()
            .setSkullTexture(pet.entityTexture)
            .build()

        return spawnPetItemDisplay(location, pet, skull)
    }
}

internal fun spawnPetItemDisplay(
    location: Location,
    pet: Pet,
    item: ItemStack
): ItemDisplay = location.world!!.spawn(location, ItemDisplay::class.java) {
    it.setItemStack(item)
    it.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
    it.isCustomNameVisible = true
    @Suppress("DEPRECATION")
    it.customName = pet.name

    // Folia can't remove pets in other regions on disable, so they must not be saved.
    if (Prerequisite.HAS_FOLIA.isMet) {
        it.isPersistent = false
    }

    val smoothingTicks = plugin.configYml
        .getInt("pet-entity.item-display.teleport-duration", DEFAULT_SMOOTHING_TICKS)
        .coerceIn(MIN_SMOOTHING_TICKS, MAX_SMOOTHING_TICKS)
    it.teleportDuration = smoothingTicks
    it.interpolationDuration = smoothingTicks

    val scale = plugin.configYml.getDouble("pet-entity.scale")
    if (scale in MIN_PET_SCALE..MAX_PET_SCALE) {
        val transform = it.transformation
        transform.scale.set(scale, scale, scale)
        it.transformation = transform
    }
}
