package com.willfp.ecopets.commands

import com.willfp.ecopets.api.event.PetAdoptEvent
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.hasPet
import com.willfp.ecopets.pets.setPetLevel
import com.willfp.ecopets.runOwnedIfOnline
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.game.entity.player.PlayerUtils

/**
 * Gives on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.giveCommand() = subcommand(
    name = "give",
    permission = "ecopets.command.give"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val petId = argument(1, "needs-pet")

        val player = knownPlayer(playerName) ?: fail("invalid-player")
        val pet = Pets.getByID(petId) ?: fail("invalid-pet")

        if (player.hasPet(pet)) {
            fail("already-has-pet")
        }

        player.runOwnedIfOnline {
            val event = PetAdoptEvent(player, pet)
            Bukkit.getPluginManager().callEvent(event)

            if (event.isCancelled) {
                reply("cancelled-adoption")
            } else {
                player.setPetLevel(pet, 1)
                replyWithoutPlaceholders(
                    "gave-pet",
                    "player" to PlayerUtils.getSavedDisplayName(player),
                    "pet" to pet.name
                )
            }
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
