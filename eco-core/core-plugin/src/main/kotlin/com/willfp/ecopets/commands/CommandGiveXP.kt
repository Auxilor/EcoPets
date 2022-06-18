package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.savedDisplayName
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
            sender.sendMessage(plugin.langYml.getMessage("needs-pet"))
            return
        }
        if (args.size == 1) {
            sender.sendMessage(plugin.langYml.getMessage("needs-amount"))
            return
        }
        var player = sender
        if (args.size == 3){
            player = Bukkit.getPlayer(args[2])!!
        }
        if (!(player is Player)) return;
        val pet = Pets.getByID(args[0])

        if (pet == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-pet"))
            return
        }
        if (!player.hasPet(pet)) {
            sender.sendMessage(plugin.langYml.getMessage("doenst-have-pet"))
            return
        }
        player.givePetExperience(
            pet,
            args[1].toDouble()
        )
        sender.sendMessage(
            plugin.langYml.getMessage("give-xp", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%player%", player.savedDisplayName)
                .replace("%xp%", args[1])
                .replace("%pet%", pet.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            return Pets.values().map { it.id }
        }
        if (args.size == 2) {
            return listOf("10", "100", "1000", "10000")
        }

        if (args.size == 3) {
            return Bukkit.getOnlinePlayers().map { it.name }
        }



        return emptyList()
    }
}