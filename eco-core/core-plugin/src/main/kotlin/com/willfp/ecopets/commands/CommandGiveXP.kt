package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
import com.willfp.eco.util.toNiceString
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.givePetExperience
import com.willfp.ecopets.pets.hasPet
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandGiveXP(plugin: EcoPlugin) : Subcommand(plugin, "givexp", "ecopets.command.givexp", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("needs-player"))
            return
        }

        if (args.size == 1) {
            sender.sendMessage(plugin.langYml.getMessage("needs-pet"))
            return
        }

        if (args.size == 2) {
            sender.sendMessage(plugin.langYml.getMessage("needs-amount"))
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

        if (!player.hasPet(pet)) {
            sender.sendMessage(plugin.langYml.getMessage("doesnt-have-pet"))
            return
        }

        val amount = args[2].toDoubleOrNull()

        if (amount == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
            return
        }

        player.givePetExperience(
            pet,
            amount
        )

        sender.sendMessage(
            plugin.langYml.getMessage("gave-xp", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%player%", player.savedDisplayName)
                .replace("%xp%", amount.toNiceString())
                .replace("%pet%", pet.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers().map { it.name }
        }

        if (args.size == 2) {
            return Pets.values().map { it.id }
        }

        if (args.size == 3) {
            return listOf("10", "100", "1000", "10000")
        }

        return emptyList()
    }
}