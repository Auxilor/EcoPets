package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import org.bukkit.command.CommandSender

class CommandEcopets(plugin: EcoPlugin) : PluginCommand(plugin, "ecopets", "ecopets.command.ecopets", false) {
    init {
        this.addSubcommand(CommandReload(plugin))
            .addSubcommand(CommandGive(plugin))
            .addSubcommand(CommandGiveEgg(plugin))
            .addSubcommand(CommandGiveXP(plugin))
            .addSubcommand(CommandReset(plugin))
            .addSubcommand(CommandTake(plugin))
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}