package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot

abstract class PetEntity(
    val pet: Pet
) {
    abstract fun spawn(location: Location): ArmorStand

    companion object {
        private val registrations = mutableMapOf<String, (Pet, String) -> PetEntity>()

        @JvmStatic
        fun registerPetEntity(id: String, parse: (Pet, String) -> PetEntity) {
            registrations[id] = parse
        }

        @JvmStatic
        fun create(pet: Pet): PetEntity {
            val texture = pet.entityTexture

            if (!texture.contains(":")) {
                return SkullPetEntity(pet)
            }

            val id = texture.split(":")[0]
            val parse = registrations[id] ?: return SkullPetEntity(pet)
            return parse(pet, texture.removePrefix("$id:"))
        }
    }
}

private fun ArmorStand.applyScale(plugin: EcoPlugin, isSkull: Boolean) {
    if (!isSkull) return // Only apply scale if it's a skull

    val scale = plugin.configYml.getDouble("pet-entity.scale")

    if (scale !in 0.0625..16.0) {
        plugin.logger.warning("Invalid scale value '$scale' in config.yml. Must be between 0.0625 and 16.")
        return
    }

    val scaleAttribute = try {
        Attribute::class.java.getField("SCALE").get(null) as? Attribute
    } catch (_: NoSuchFieldException) {
        null
    }

    if (scaleAttribute != null) {
        val attr = getAttribute(scaleAttribute)
        if (attr != null) {
            attr.baseValue = scale
        } else {
            plugin.logger.warning("Failed to set scale - SCALE attribute not found on ArmorStand instance")
        }
    }
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

        for (slot in EquipmentSlot.values()) {
            stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
        }

        isCustomNameVisible = true
        @Suppress("DEPRECATION")
        customName = pet.name

        applyScale(EcoPetsPlugin.instance, isSkull)

    }

    return stand
}
