package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.hasPet
import com.willfp.ecopets.pets.setPetLevel
import com.willfp.ecopets.pets.setPetXP
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.game.entity.player.PlayerUtils
import wiki.ecohub.game.entity.runOwned

/**
 * Resets on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.resetCommand() = subcommand(
    name = "reset",
    permission = "ecopets.command.reset"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val petId = argument(1, "needs-pet")

        val player = Bukkit.getPlayer(playerName) ?: fail("invalid-player")
        val pet = Pets.getByID(petId) ?: fail("invalid-pet")

        if (!player.hasPet(pet)) {
            fail("doesnt-have-pet")
        }

        player.runOwned {
            player.setPetXP(pet, 0.0)
            player.setPetLevel(pet, 1)

            replyWithoutPlaceholders(
                "reset-xp",
                "player" to PlayerUtils.getSavedDisplayName(player),
                "pet" to pet.name
            )
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> Pets.values().map { it.id }
            else -> emptyList()
        }
    }
}
