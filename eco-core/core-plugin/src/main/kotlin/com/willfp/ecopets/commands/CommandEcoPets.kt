package com.willfp.ecopets.commands

import com.willfp.ecopets.EcoPetsPlugin
import wiki.ecohub.core.command.command

internal fun EcoPetsPlugin.ecoPetsCommand() = command(name = "ecopets", permission = "ecopets.command.ecopets") {
    reloadCommand()
    giveCommand()
    takeCommand()
    giveEggCommand()
    giveXPCommand()
    resetCommand()
    giveCurrentXPCommand()
    activateOtherCommand()
    deactivateOtherCommand()
}
