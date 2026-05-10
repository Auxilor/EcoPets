package com.willfp.ecopets.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import com.willfp.ecopets.pets.setPetLevel
import com.willfp.ecopets.pets.setPetXP
import com.willfp.ecopets.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object CommandTake : Subcommand(
    plugin,
    "take",
    "ecopets.command.take",
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

        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(playerName)

        if (!player.hasPlayedBefore() && player !is Player) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        val pet = Pets.getByID(args[1])

        if (pet == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-pet"))
            return
        }

        if (!player.hasPet(pet)) {
            sender.sendMessage(plugin.langYml.getMessage("doesnt-have-pet"))
            return
        }

        val onlinePlayer = player.player
        if (onlinePlayer != null && onlinePlayer.activePet == pet) {
            onlinePlayer.activePet = null
        }

        player.setPetXP(pet, 0.0)
        player.setPetLevel(pet, 0)

        sender.sendMessage(
            plugin.langYml.getMessage("took-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%player%", player.savedDisplayName)
                .replace("%pet%", pet.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()
        if (args.isEmpty()) {
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
            @Suppress("DEPRECATION")
            val player = Bukkit.getOfflinePlayer(args[0])
            StringUtil.copyPartialMatches(
                args[1],
                Pets.values().filter { player.hasPet(it) }.map { it.id },
                completions
            )
            return completions
        }

        return emptyList()
    }
}