package com.willfp.ecopets.commands

import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.hasPet
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext

internal fun CommandBuilder<Player>.activateCommand() = playerSubcommand(
    name = "activate",
    permission = "ecopets.command.activate"
) {
    executes {
        val pet = Pets.getByID(argument(0, "needs-pet"))?.takeIf { sender.hasPet(it) } ?: fail("invalid-pet")

        if (sender.activePet == pet) {
            fail("pet-already-active")
        }

        if (pet.canActivate(sender)) {
            activate(pet)
        } else {
            replyWithoutPlaceholders("cannot-activate-pet", "pet" to pet.name)
        }
    }

    completes {
        if (args.size == 1) Pets.values().filter { sender.hasPet(it) }.map { it.id } else emptyList()
    }
}

private fun CommandContext<Player>.activate(pet: Pet) {
    if (!canSwapTo(sender, pet)) {
        fail("cancelled-swap")
    }

    sender.activePet = pet

    if (sender.activePet == pet) {
        replyWithoutPlaceholders("activated-pet", "pet" to pet.name)
    }
}
