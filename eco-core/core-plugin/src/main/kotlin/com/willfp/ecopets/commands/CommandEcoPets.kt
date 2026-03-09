package com.willfp.ecopets.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecopets.plugin
import org.bukkit.command.CommandSender

object CommandEcoPets : PluginCommand(
    plugin,
    "ecopets",
    "ecopets.command.ecopets",
    false
) {
    init {
        this.addSubcommand(CommandReload)
            .addSubcommand(CommandGive)
            .addSubcommand(CommandGiveEgg)
            .addSubcommand(CommandGiveXP)
            .addSubcommand(CommandReset)
            .addSubcommand(CommandGiveCurrentXP)
            .addSubcommand(CommandActivateOther)
            .addSubcommand(CommandDeactivateOther)
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}