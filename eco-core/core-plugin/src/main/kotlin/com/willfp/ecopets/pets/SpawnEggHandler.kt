package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class SpawnEggHandler(
    private val plugin: EcoPlugin
) : Listener {
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

        if (player.hasPet(pet)) {
            player.sendMessage(plugin.langYml.getMessage("cannot-spawn-pet"))
            return
        }

        if (event.hand == EquipmentSlot.HAND) {
            val hand = event.player.inventory.itemInMainHand
            hand.amount = hand.amount - 1
        } else {
            val hand = event.player.inventory.itemInOffHand
            hand.amount = hand.amount - 1
        }

        player.setPetLevel(pet, 1)
    }
}