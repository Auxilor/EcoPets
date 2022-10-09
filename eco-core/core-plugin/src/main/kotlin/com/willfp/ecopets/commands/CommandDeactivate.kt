package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.pets.activePet
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandDeactivate(plugin: EcoPlugin) : Subcommand(plugin, "deactivate", "ecopets.command.deactivate", true) {
    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player

        if (player.activePet == null) {
            player.sendMessage(plugin.langYml.getMessage("no-pet-active"))
            return
        }

        player.sendMessage(
            plugin.langYml.getMessage("deactivated-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%pet%", player.activePet?.name ?: "")
        )

        player.activePet = null
    }
}
