package com.willfp.ecopets.pets

import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.plugin
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

object SpawnEggHandler : Listener {
    @EventHandler(
        ignoreCancelled = true
    )
    fun handle(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val player = event.player

        val item = event.item ?: return
        val pet = item.petEgg ?: return

        event.isCancelled = true
        event.setUseItemInHand(Event.Result.DENY)

        val eggLevel = item.petEggLevel
        val eggXp = item.petEggXp
        val ownedLevel = player.getPetLevel(pet)

        if (ownedLevel > 0) {
            val policy = plugin.configYml.getString("pet-egg.redeem-conflict").lowercase()
            if (policy == "reject" || eggLevel <= ownedLevel) {
                player.sendMessage(
                    if (policy == "reject") plugin.langYml.getMessage("cannot-spawn-pet")
                    else plugin.langYml.getMessage("egg-lower-level")
                )
                return
            }
        }

        if (event.hand == EquipmentSlot.HAND) {
            event.player.inventory.itemInMainHand.amount -= 1
        } else {
            event.player.inventory.itemInOffHand.amount -= 1
        }

        player.setPetLevel(pet, maxOf(eggLevel, 1))
        player.setPetXP(pet, eggXp)
        player.sendMessage(
            plugin.langYml.getMessage("pet-spawned", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%pet%", pet.name)
        )
    }
}