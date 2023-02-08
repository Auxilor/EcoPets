package com.willfp.ecopets.api.event

import com.willfp.ecopets.pets.Pet

interface PetEvent {
    val pet: Pet
}
