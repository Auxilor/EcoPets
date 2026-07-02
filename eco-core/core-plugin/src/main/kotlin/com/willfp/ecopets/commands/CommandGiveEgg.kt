package com.willfp.ecopets.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
import com.willfp.eco.util.toNiceString
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandGiveEgg : Subcommand(
    plugin,
    "giveegg",
    "ecopets.command.giveegg",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("needs-player"))
            return
        }

        if (args.size == 1) {
            sender.sendMessage(plugin.langYml.getMessage("needs-pet"))
            return
        }

        val playerName = args[0]

        val player = Bukkit.getPlayer(playerName)

        if (player == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        val pet = Pets.getByID(args[1])

        if (pet == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-pet"))
            return
        }

        val level = args.getOrNull(2)?.toIntOrNull() ?: 1
        val xp = args.getOrNull(3)?.toDoubleOrNull() ?: 0.0

        val egg = pet.makeSpawnEgg(level, xp)

        if (egg == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-pet"))
            return
        }

        DropQueue(player)
            .addItem(egg)
            .forceTelekinesis()
            .push()

        sender.sendMessage(
            plugin.langYml.getMessage("gave-pet-egg", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%player%", player.savedDisplayName)
                .replace("%pet%", pet.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()
        if (args.isEmpty()) {
            // Currently, this case is not ever reached
            return Bukkit.getOnlinePlayers().map { it.name }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )
            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                Pets.values().map { it.id },
                completions
            )
            return completions
        }

        if (args.size == 3) {
            val pet = Pets.getByID(args[1])
            val maxLevel = pet?.maxLevel ?: 1
            return listOf("1", "10", "50", maxLevel.toString()).distinct()
        }

        if (args.size == 4) {
            val pet = Pets.getByID(args[1])
            val level = args[2].toIntOrNull() ?: 1
            val maxXp = pet?.getExpForLevel(level)?.takeIf { it.isFinite() }

            val options = listOf(10.0, 50.0, 100.0, 10000.0)
            return if (maxXp != null) {
                (options.filter { it < maxXp } + maxXp).distinct().map { it.toNiceString() }
            } else {
                options.map { it.toNiceString() }
            }
        }

        return emptyList()
    }
}