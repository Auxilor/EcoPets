package com.willfp.ecopets.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecopets.pets.PetsGUI
import com.willfp.ecopets.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandPets : PluginCommand(
    plugin,
    "pets",
    "ecopets.command.pets",
    true
) {
    init {
        this.addSubcommand(CommandActivate)
            .addSubcommand(CommandDeactivate)
    }

    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player
        PetsGUI.open(player)
    }
}
