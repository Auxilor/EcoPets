package com.willfp.ecopets.commands

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.PetsGUI
import wiki.ecohub.core.command.playerCommand

internal fun EcoPetsPlugin.petsCommand() = playerCommand(name = "pets", permission = "ecopets.command.pets") {
    activateCommand()
    deactivateCommand()

    executes {
        PetsGUI.open(sender)
    }
}
