package com.willfp.ecopets.commands

import com.willfp.ecopets.api.event.PlayerPetSwapEvent
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.pets.activePet
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.ui.text.StringUtils

internal val xpCompletions = listOf("10", "100", "1000", "10000")

/**
 * The player called [name], if they are online or have joined before.
 */
@Suppress("DEPRECATION")
internal fun knownPlayer(name: String): OfflinePlayer? =
    Bukkit.getOfflinePlayer(name).takeIf { it.hasPlayedBefore() || it is Player }

/**
 * If [player] may swap to [pet]. Calls [PlayerPetSwapEvent] when a pet is already active.
 */
internal fun canSwapTo(player: OfflinePlayer, pet: Pet): Boolean {
    val oldPet = player.activePet ?: return true
    val event = PlayerPetSwapEvent(player, pet, oldPet)
    Bukkit.getPluginManager().callEvent(event)
    return !event.isCancelled
}

/**
 * Send the lang message [key] with [replacements], without translating placeholders in it first.
 */
internal fun CommandContext<*>.replyWithoutPlaceholders(key: String, vararg replacements: Pair<String, String>) {
    val message = plugin.langYml.getMessage(key, StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)

    sender.sendMessage(
        replacements.fold(message) { replaced, (placeholder, value) ->
            replaced.replace(oldValue = "%$placeholder%", newValue = value)
        }
    )
}
