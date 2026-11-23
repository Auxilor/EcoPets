package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.runOwnedIfOnline
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext

/**
 * Deactivates on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.deactivateOtherCommand() = subcommand(
    name = "deactivate",
    permission = "ecopets.command.deactivateother"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val player = knownPlayer(playerName)

        when {
            player == null -> reply("invalid-player", "player" to playerName)
            player.activePet == null -> reply("no-pet-active", "player" to playerName)
            else -> deactivateFor(player, playerName)
        }
    }

    completes {
        if (args.size == 1) playerNames() else emptyList()
    }
}

private fun CommandContext<CommandSender>.deactivateFor(player: OfflinePlayer, playerName: String) {
    player.runOwnedIfOnline {
        replyWithoutPlaceholders("deactivated-pet", "pet" to player.activePet?.name.orEmpty(), "player" to playerName)

        player.activePet = null
    }
}
