package com.willfp.ecopets

import com.willfp.ecopets.api.EcoPetsAPI
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.getPetLevel
import com.willfp.ecopets.pets.getPetProgress
import com.willfp.ecopets.pets.getPetXP
import com.willfp.ecopets.pets.getPetXPRequired
import com.willfp.ecopets.pets.givePetExperience
import com.willfp.ecopets.pets.hasPet
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

internal object EcoPetsAPIImpl : EcoPetsAPI {
    override fun hasPet(player: OfflinePlayer, pet: Pet) = player.hasPet(pet)

    override fun getActivePet(player: OfflinePlayer): Pet? = player.activePet

    override fun setActivePet(player: OfflinePlayer, pet: Pet?) {
        player.activePet = pet
    }

    override fun getPetLevel(player: OfflinePlayer, pet: Pet) = player.getPetLevel(pet)

    override fun givePetExperience(player: Player, pet: Pet, amount: Double) =
        player.givePetExperience(pet, amount)

    override fun givePetExperience(player: Player, pet: Pet, amount: Double, applyMultipliers: Boolean) =
        player.givePetExperience(pet, amount, noMultiply = !applyMultipliers)

    override fun getPetProgress(player: OfflinePlayer, pet: Pet) =
        player.getPetProgress(pet)

    override fun getPetXPRequired(player: OfflinePlayer, pet: Pet) =
        player.getPetXPRequired(pet)

    override fun getPetXP(player: OfflinePlayer, pet: Pet) =
        player.getPetXP(pet)
}
