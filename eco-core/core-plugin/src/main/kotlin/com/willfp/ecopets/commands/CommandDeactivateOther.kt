package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandDeactivateOther(plugin: EcoPlugin) :
    Subcommand(plugin, "deactivateother", "ecopets.command.deactivateother", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("needs-player"))
            return
        }

        val playerName = args[0]

        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(playerName)

        if (!player.hasPlayedBefore() && player !is Player) {
            sender.sendMessage(
                plugin.langYml.getMessage("invalid-player")
                    .replace("%player%", playerName)
            )
            return
        }

        if (player.activePet == null) {
            sender.sendMessage(
                plugin.langYml.getMessage("no-pet-active")
                    .replace("%player%", playerName)
            )
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("deactivated-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%pet%", player.activePet?.name ?: "")
                .replace("%player%", playerName)
        )

        player.activePet = null
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers().map { it.name }
        }

        return emptyList()
    }
}
