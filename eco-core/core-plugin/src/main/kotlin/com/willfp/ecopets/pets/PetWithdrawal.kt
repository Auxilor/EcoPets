package com.willfp.ecopets.pets

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.plugin
import org.bukkit.entity.Player

enum class WithdrawResult {
    OK, NO_ACTIVE, NOT_WITHDRAWABLE, CANNOT_AFFORD
}

fun canWithdraw(player: Player, pet: Pet?): WithdrawResult {
    if (pet == null) return WithdrawResult.NO_ACTIVE
    if (!pet.withdrawable) return WithdrawResult.NOT_WITHDRAWABLE
    if (!pet.withdrawPrice.canAfford(player)) return WithdrawResult.CANNOT_AFFORD
    return WithdrawResult.OK
}

/** Sends the appropriate lang message for a non-OK result. */
fun WithdrawResult.notifyFail(player: Player, pet: Pet?) {
    when (this) {
        WithdrawResult.NO_ACTIVE ->
            player.sendMessage(plugin.langYml.getMessage("no-active-pet-to-withdraw"))
        WithdrawResult.NOT_WITHDRAWABLE ->
            player.sendMessage(plugin.langYml.getMessage("pet-not-withdrawable"))
        WithdrawResult.CANNOT_AFFORD ->
            player.sendMessage(
                plugin.langYml.getMessage("cannot-afford-withdraw", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                    .replace("%price%", pet?.withdrawPrice?.getDisplay(player) ?: "")
            )
        WithdrawResult.OK -> {}
    }
}

/** Performs the withdrawal. Returns true on success. Re-validates before acting. */
fun withdrawPet(player: Player, pet: Pet): Boolean {
    val result = canWithdraw(player, pet)
    if (result != WithdrawResult.OK) {
        result.notifyFail(player, pet)
        return false
    }
    if (player.activePet != pet) {
        player.sendMessage(plugin.langYml.getMessage("no-active-pet-to-withdraw"))
        return false
    }

    pet.withdrawPrice.pay(player)

    val level = player.getPetLevel(pet)
    val xp = player.getPetXP(pet)
    val egg = pet.makeSpawnEgg(level, xp) ?: run {
        player.sendMessage(plugin.langYml.getMessage("pet-not-withdrawable"))
        return false
    }

    DropQueue(player)
        .addItem(egg)
        .forceTelekinesis()
        .push()

    player.setPetLevel(pet, 0)
    player.setPetXP(pet, 0.0)
    player.activePet = null

    player.sendMessage(
        plugin.langYml.getMessage("pet-withdrawn", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
            .replace("%pet%", pet.name)
    )
    return true
}
