package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandActivateOther(plugin: EcoPlugin) :
    Subcommand(plugin, "activateother", "ecopets.command.activateother", false) {
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
            sender.sendMessage(
                plugin.langYml.getMessage("invalid-player")
                    .replace("%player%", playerName)
            )
            return
        }

        val pet = Pets.getByID(args[1])

        if (pet == null) {
            sender.sendMessage(
                plugin.langYml.getMessage("invalid-pet")
                    .replace("%player%", playerName)
            )
            return
        }

        if (!player.hasPet(pet)) {
            sender.sendMessage(
                plugin.langYml.getMessage("doesnt-have-pet")
                    .replace("%player%", playerName)
            )
            return
        }

        if (player.activePet == pet) {
            sender.sendMessage(
                plugin.langYml.getMessage("pet-already-active")
                    .replace("%player%", playerName)
            )
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("activated-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%pet%", pet.name)
                .replace("%player%", playerName)
        )
        player.activePet = pet
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers().map { it.name }
        }

        if (args.size == 2) {
            return Pets.values().map { it.id }
        }

        return emptyList()
    }
}
