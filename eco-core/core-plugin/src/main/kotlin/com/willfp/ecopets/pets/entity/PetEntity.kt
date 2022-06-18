package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

abstract class PetEntity(
    val pet: Pet
) {
    abstract fun spawn(location: Location): ArmorStand
}
