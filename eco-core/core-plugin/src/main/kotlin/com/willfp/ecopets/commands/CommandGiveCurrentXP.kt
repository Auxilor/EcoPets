package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.givePetExperience
import com.willfp.ecopets.pets.hasPet
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.game.entity.player.PlayerUtils
import wiki.ecohub.game.entity.runOwned

/**
 * Gives on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.giveCurrentXPCommand() = subcommand(
    name = "givecurrentxp",
    permission = "ecopets.command.givecurrentxp"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val amountArgument = argument(1, "needs-amount")

        val player = Bukkit.getPlayer(playerName) ?: fail("invalid-player")
        val pet = player.activePet ?: fail("no-pet")

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
                "gave-current-xp",
                "player" to PlayerUtils.getSavedDisplayName(player),
                "xp" to amount.toNiceString()
            )
        }
    }

    completes {
        when (args.size) {
            1 -> playerNames()
            2 -> xpCompletions
            else -> emptyList()
        }
    }
}
