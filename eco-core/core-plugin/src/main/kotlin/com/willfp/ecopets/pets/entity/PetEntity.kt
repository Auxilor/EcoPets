package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot

abstract class PetEntity(
    val pet: Pet
) {
    abstract fun spawn(location: Location): Entity

    companion object {
        private val registrations = mutableMapOf<String, (Pet, String) -> PetEntity>()

        @JvmStatic
        fun registerPetEntity(id: String, parse: (Pet, String) -> PetEntity) {
            registrations[id] = parse
        }

        @JvmStatic
        fun create(plugin: EcoPetsPlugin, pet: Pet): PetEntity {
            val texture = pet.entityTexture

            if (!texture.contains(":")) {
                if (plugin.configYml.getBool("pet-entity.item-display.enabled")) {
                    return ItemDisplayPetEntity(pet, plugin)
                }
                return SkullPetEntity(pet)
            }

            val id = texture.split(":")[0]
            val parse = registrations[id] ?: return SkullPetEntity(pet)
            return parse(pet, texture.removePrefix("$id:"))
        }
    }
}

internal fun emptyArmorStandAt(location: Location, pet: Pet): ArmorStand {
    val stand = location.world!!.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand

    stand.isVisible = false
    stand.isInvulnerable = true
    stand.isSmall = true
    stand.setGravity(false)
    stand.isCollidable = false
    stand.isPersistent = false

    for (slot in EquipmentSlot.values()) {
        stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
    }

    stand.isCustomNameVisible = true
    @Suppress("DEPRECATION")
    stand.customName = pet.name

    return stand
}
