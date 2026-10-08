package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.Items
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.plugin
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

// Shared range matches Bukkit's SCALE attribute limits used by ArmorStand pets.
internal const val DEFAULT_PET_SCALE = 1.0
internal const val MIN_PET_SCALE = 0.0625
internal const val MAX_PET_SCALE = 16.0

abstract class PetEntity(
    val pet: Pet
) {
    abstract fun spawn(location: Location): Entity

    companion object {
        private val registrations = ConcurrentHashMap<String, (Pet, String) -> PetEntity>()

        init {
            registrations["item"] = ::itemPetEntity
        }

        @JvmStatic
        fun registerPetEntity(id: String, parse: (Pet, String) -> PetEntity) {
            registrations[id] = parse
        }

        @JvmStatic
        fun create(pet: Pet): PetEntity {
            val texture = pet.entityTexture

            if (!texture.contains(":")) {
                if (plugin.configYml.getBool("pet-entity.item-display.enabled")) {
                    return ItemDisplayPetEntity(pet)
                }
                return SkullPetEntity(pet)
            }

            val id = texture.split(":")[0]
            val parse = registrations[id] ?: return SkullPetEntity(pet)
            return parse(pet, texture.removePrefix("$id:"))
        }
    }
}

private fun ArmorStand.applyScale(isSkull: Boolean) {
    if (!isSkull) return // Only apply scale if it's a skull

    val scale = plugin.configYml.getDouble("pet-entity.scale")

    if (scale !in MIN_PET_SCALE..MAX_PET_SCALE) {
        plugin.logger.warning(
            "Invalid scale value '$scale' in config.yml. " +
                "Must be between $MIN_PET_SCALE and $MAX_PET_SCALE."
        )
        return
    }

    val scaleAttribute = getAttribute(Attribute.SCALE)
    if (scaleAttribute == null) {
        plugin.logger.warning("Failed to set scale - SCALE attribute not found on ArmorStand")
        return
    }

    scaleAttribute.baseValue = scale

}

internal fun emptyArmorStandAt(location: Location, pet: Pet, isSkull: Boolean): ArmorStand {
    val stand = location.world!!.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand

    stand.apply {
        isVisible = false
        isInvulnerable = true
        isSmall = true
        setGravity(false)
        isCollidable = false
        isPersistent = false

        for (slot in EquipmentSlot.entries) {
            stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
        }

        isCustomNameVisible = true
        @Suppress("DEPRECATION")
        customName = pet.name

        applyScale(isSkull)

    }

    return stand
}

internal fun lookupItem(id: String): ItemStack {
    val lookup = Items.lookup(id)
    if (lookup is EmptyTestableItem) {
        return ItemStack(Material.BARRIER)
    }
    return lookup.item ?: ItemStack(Material.BARRIER)
}

private fun itemPetEntity(pet: Pet, itemLookup: String): PetEntity = object : PetEntity(pet) {
    override fun spawn(location: Location): Entity {
        val item = lookupItem(itemLookup)
        return if (plugin.configYml.getBool("pet-entity.item-display.enabled")) {
            spawnPetItemDisplay(location, pet, item)
        } else {
            spawnAsArmorStand(location, item)
        }
    }

    private fun spawnAsArmorStand(location: Location, item: ItemStack): Entity {
        val stand = emptyArmorStandAt(location, pet, isSkull = true)
        stand.equipment.helmet = item
        return stand
    }
}
