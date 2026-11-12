package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.activePet
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandBuilder

internal fun CommandBuilder<Player>.deactivateCommand() = playerSubcommand(
    name = "deactivate",
    permission = "ecopets.command.deactivate"
) {
    executes {
        val pet = sender.activePet ?: fail("no-pet-active")

        replyWithoutPlaceholders("deactivated-pet", "pet" to pet.name)

        sender.activePet = null
    }
}
