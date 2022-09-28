package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecopets.pets.PetsGUI
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandPets(plugin: EcoPlugin) : PluginCommand(plugin, "pets", "ecopets.command.pets", true) {
    init {
        this.addSubcommand(CommandActivate(plugin))
            .addSubcommand(CommandDeactivate(plugin))
    }

    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player
        PetsGUI.open(player)
    }
}
