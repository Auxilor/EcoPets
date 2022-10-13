package com.willfp.ecopets.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecopets.pets.Pets
import com.willfp.libreforge.LibReforgePlugin
import com.willfp.libreforge.lrcdb.CommandExport
import com.willfp.libreforge.lrcdb.CommandImport
import com.willfp.libreforge.lrcdb.ExportableConfig
import org.bukkit.command.CommandSender

class CommandEcopets(plugin: LibReforgePlugin) : PluginCommand(plugin, "ecopets", "ecopets.command.ecopets", false) {
    init {
        this.addSubcommand(CommandReload(plugin))
            .addSubcommand(CommandGive(plugin))
            .addSubcommand(CommandGiveEgg(plugin))
            .addSubcommand(CommandGiveXP(plugin))
            .addSubcommand(CommandReset(plugin))
            .addSubcommand(CommandImport("pets", plugin))
            .addSubcommand(CommandExport(plugin) {
                Pets.values().map {
                    ExportableConfig(
                        it.id,
                        it.config
                    )
                }
            })
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("invalid-command")
        )
    }
}