package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

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
