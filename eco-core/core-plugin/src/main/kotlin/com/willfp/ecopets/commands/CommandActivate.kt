package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandActivate(plugin: EcoPlugin) : Subcommand(plugin, "activate", "ecopets.command.activate", true) {
    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player

        if (args.isEmpty()) {
            player.sendMessage(plugin.langYml.getMessage("needs-pet"))
            return
        }

        val id = args[0]

        val pet = Pets.getByID(id)

        if (pet == null || !player.hasPet(pet)) {
            player.sendMessage(plugin.langYml.getMessage("invalid-pet"))
            return
        }

        if (player.activePet == pet) {
            player.sendMessage(plugin.langYml.getMessage("pet-already-active"))
            return
        }

        player.sendMessage(
            plugin.langYml.getMessage("activated-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%pet%", pet.name)
        )
        player.activePet = pet
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (sender !is Player) {
            return emptyList()
        }

        val completions = mutableListOf<String>()
        if (args.isEmpty()) {
            // Currently, this case is not ever reached
            return Pets.values().filter { sender.hasPet(it) }.map { it.id }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Pets.values().filter { sender.hasPet(it) }.map { it.id },
                completions
            )
            return completions
        }

        return emptyList()
    }
}
