package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import com.willfp.ecopets.runOwnedIfOnline
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext

/**
 * Activates on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.activateOtherCommand() = subcommand(
    name = "activate",
    permission = "ecopets.command.activateother"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val pet = Pets.getByID(argument(1, "needs-pet"))
        val player = knownPlayer(playerName)

        when {
            player == null -> reply("invalid-player", "player" to playerName)
            pet == null -> reply("invalid-pet", "player" to playerName)
            !player.hasPet(pet) -> reply("doesnt-have-pet", "player" to playerName)
            player.activePet == pet -> reply("pet-already-active", "player" to playerName)
            else -> activateFor(player, pet, playerName)
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

private fun CommandContext<CommandSender>.activateFor(player: OfflinePlayer, pet: Pet, playerName: String) {
    player.runOwnedIfOnline {
        if (!canSwapTo(player, pet)) {
            reply("cancelled-swap-other")
        } else {
            player.activePet = pet

            if (player.activePet == pet) {
                replyWithoutPlaceholders("activated-pet", "pet" to pet.name, "player" to playerName)
            }
        }
    }
}
