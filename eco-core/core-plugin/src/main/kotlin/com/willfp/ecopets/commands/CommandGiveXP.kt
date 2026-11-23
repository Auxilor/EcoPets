package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.givePetExperience
import com.willfp.ecopets.pets.hasPet
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.game.entity.player.PlayerUtils
import wiki.ecohub.game.entity.runOwned

private const val AMOUNT_ARGUMENT = 2

/**
 * Gives on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.giveXPCommand() = subcommand(
    name = "givexp",
    permission = "ecopets.command.givexp"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val petId = argument(1, "needs-pet")
        val amountArgument = argument(AMOUNT_ARGUMENT, "needs-amount")

        val player = Bukkit.getPlayer(playerName) ?: fail("invalid-player")
        val pet = Pets.getByID(petId) ?: fail("invalid-pet")

        if (!player.hasPet(pet)) {
            fail("doesnt-have-pet")
        }

        val amount = amountArgument.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: fail("invalid-amount")

        player.runOwned {
            player.givePetExperience(
                pet,
                amount
            )

            replyWithoutPlaceholders(
                "gave-xp",
                "player" to PlayerUtils.getSavedDisplayName(player),
                "xp" to amount.toNiceString(),
                "pet" to pet.name
            )
        }
    }

    completes {
        when (args.lastIndex) {
            0 -> playerNames()
            1 -> Pets.values().map { it.id }
            AMOUNT_ARGUMENT -> xpCompletions
            else -> emptyList()
        }
    }
}
