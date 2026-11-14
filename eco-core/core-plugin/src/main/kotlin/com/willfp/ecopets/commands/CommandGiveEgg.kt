package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pets
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.game.drop.DropQueue
import wiki.ecohub.game.entity.player.PlayerUtils
import wiki.ecohub.game.entity.runOwned

private const val LEVEL_ARGUMENT = 2

private const val XP_ARGUMENT = 3

private val levelCompletions = listOf("1", "10", "50")

private val eggXpCompletions = listOf(10.0, 50.0, 100.0, 10_000.0)

/**
 * Gives on the player's region, as the console runs off it on Folia.
 */
internal fun CommandBuilder<CommandSender>.giveEggCommand() = subcommand(
    name = "giveegg",
    permission = "ecopets.command.giveegg"
) {
    executes {
        val playerName = argument(0, "needs-player")
        val petId = argument(1, "needs-pet")

        val player = Bukkit.getPlayer(playerName) ?: fail("invalid-player")
        val pet = Pets.getByID(petId) ?: fail("invalid-pet")

        val level = args.getOrNull(LEVEL_ARGUMENT)?.toIntOrNull() ?: 1
        val xp = args.getOrNull(XP_ARGUMENT)?.toDoubleOrNull() ?: 0.0

        if (level !in 1..pet.maxLevel) {
            fail("invalid-level")
        }

        if (!xp.isFinite() || xp < 0.0) {
            fail("invalid-xp")
        }

        val egg = pet.makeSpawnEgg(level, xp) ?: fail("invalid-pet")

        player.runOwned {
            DropQueue(player)
                .addItem(egg)
                .forceTelekinesis()
                .push()

            replyWithoutPlaceholders(
                "gave-pet-egg",
                "player" to PlayerUtils.getSavedDisplayName(player),
                "pet" to pet.name
            )
        }
    }

    completes {
        giveEggCompletions()
    }
}

private fun CommandContext<CommandSender>.giveEggCompletions(): List<String> =
    when (args.lastIndex) {
        0 -> playerNames()
        1 -> Pets.values().map { it.id }
        LEVEL_ARGUMENT -> levelCompletions + (Pets.getByID(args[1])?.maxLevel ?: 1).toString()
        XP_ARGUMENT -> xpCompletions()
        else -> emptyList()
    }

private fun CommandContext<CommandSender>.xpCompletions(): List<String> {
    val level = args[LEVEL_ARGUMENT].toIntOrNull() ?: 1
    val maxXp = Pets.getByID(args[1])?.run { getExpForLevel(level).takeIf { it.isFinite() } }
        ?: return eggXpCompletions.map { it.toNiceString() }

    return (eggXpCompletions.filter { it < maxXp } + maxXp).map { it.toNiceString() }
}
