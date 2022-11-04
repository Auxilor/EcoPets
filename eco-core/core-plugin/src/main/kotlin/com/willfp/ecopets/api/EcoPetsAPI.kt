package com.willfp.ecopets.api

import com.willfp.ecopets.EcoPetsAPIImpl
import com.willfp.ecopets.pets.Pet
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

interface EcoPetsAPI {
    /**
     * Get if a player has a pet.
     *
     * @param player The player.
     * @param pet The pet.
     * @return If the player has the pet unlocked
     */
    fun hasPet(
        player: OfflinePlayer,
        pet: Pet
    ): Boolean

    /**
     * Get a player's active pet.
     *
     * @param player The player.
     * @return The active pet.
     */
    fun getActivePet(
        player: OfflinePlayer
    ): Pet?

    /**
     * Set a player's active pet.
     *
     * @param player The player.
     * @param pet The pet.
     */
    fun setActivePet(
        player: OfflinePlayer,
        pet: Pet?
    )

    /**
     * Get a player's level of a certain pet.
     *
     * @param player The player.
     * @param pet The pet.
     * @return The level.
     */
    fun getPetLevel(
        player: OfflinePlayer,
        pet: Pet
    ): Int

    /**
     * Give pet experience to a player.
     *
     * @param player The player.
     * @param pet The pet.
     * @param amount The amount of experience to give.
     */
    fun givePetExperience(
        player: Player,
        pet: Pet,
        amount: Double
    )

    /**
     * Give pet experience to a player.
     *
     * @param player The player.
     * @param pet The pet.
     * @param amount The amount of experience to give.
     * @param applyMultipliers If multipliers should be applied.
     */
    fun givePetExperience(
        player: Player,
        pet: Pet,
        amount: Double,
        applyMultipliers: Boolean
    )

    /**
     * Get progress to next level between 0 and 1, where 0 is none and 1 is
     * complete.
     *
     * @param player The player.
     * @param pet The pet.
     * @return The progress.
     */
    fun getPetProgress(
        player: OfflinePlayer,
        pet: Pet
    ): Double

    /**
     * Get the experience required to advance to the next level.
     *
     * @param player The player.
     * @param pet The pet.
     * @return The experience required.
     */
    fun getPetXPRequired(
        player: OfflinePlayer,
        pet: Pet
    ): Int

    /**
     * Get experience to the next level.
     *
     * @param player The player.
     * @param pet The pet.
     * @return The experience.
     */
    fun getPetXP(
        player: OfflinePlayer,
        pet: Pet
    ): Double

    companion object {
        /**
         * Get the instance of the API.
         *
         * @return The API.
         */
        @JvmStatic
        val instance: EcoPetsAPI
            get() = EcoPetsAPIImpl
    }
}