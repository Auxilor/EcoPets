package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pets
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.platform.runGlobal
import wiki.ecohub.core.stdlib.toNiceString

/**
 * Reloading rebuilds shared registries, so it runs on the global region.
 */
internal fun CommandBuilder<CommandSender>.reloadCommand() = subcommand(
    name = "reload",
    permission = "ecopets.command.reload"
) {
    executes {
        Bukkit.getServer().runGlobal {
            replyWithoutPlaceholders(
                "reloaded",
                "time" to plugin.reloadWithTime().toNiceString(),
                "count" to Pets.values().size.toString()
            )
        }
    }
}
