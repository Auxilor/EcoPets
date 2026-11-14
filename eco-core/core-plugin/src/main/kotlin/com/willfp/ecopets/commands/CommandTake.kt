package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import com.willfp.ecopets.pets.setPetLevel
import com.willfp.ecopets.pets.setPetXP
import com.willfp.ecopets.runOwnedIfOnline
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.game.entity.player.PlayerUtils

/**
 * Takes on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.takeCommand() = subcommand(
    name = "take",
    permission = "ecopets.command.take"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val petId = argument(1, "needs-pet")

        val player = knownPlayer(playerName) ?: fail("invalid-player")
        val pet = Pets.getByID(petId) ?: fail("invalid-pet")

        if (!player.hasPet(pet)) {
            fail("doesnt-have-pet")
        }

        player.runOwnedIfOnline {
            val onlinePlayer = player.player
            if (onlinePlayer != null && onlinePlayer.activePet == pet) {
                onlinePlayer.activePet = null
            }

            player.setPetXP(pet, 0.0)
            player.setPetLevel(pet, 0)

            replyWithoutPlaceholders(
                "took-pet",
                "player" to PlayerUtils.getSavedDisplayName(player),
                "pet" to pet.name
            )
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> Bukkit.getPlayerExact(args[0]).let { player ->
                Pets.values().filter { player == null || player.hasPet(it) }.map { it.id }
            }
            else -> emptyList()
        }
    }
}
