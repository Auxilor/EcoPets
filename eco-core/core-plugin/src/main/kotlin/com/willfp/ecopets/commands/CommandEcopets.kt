package com.willfp.ecopets.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.libreforge.CommandFixLingeringEffects
import com.willfp.libreforge.LibReforgePlugin
import org.bukkit.command.CommandSender

class CommandEcopets(plugin: LibReforgePlugin) : PluginCommand(plugin, "ecopets", "ecopets.command.ecopets", false) {
    init {
        this.addSubcommand(CommandReload(plugin))
            .addSubcommand(CommandGive(plugin))
            .addSubcommand(CommandGiveEgg(plugin))
            .addSubcommand(CommandGiveXP(plugin))
            .addSubcommand(CommandReset(plugin))
            .addSubcommand(CommandFixLingeringEffects(plugin, "ecopets.command.fixlingeringeffects"))
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}