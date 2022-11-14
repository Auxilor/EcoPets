package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.fast.fast
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

class SpawnEggHandler(
    private val plugin: EcoPlugin
) : Listener {
    val level = plugin.namespacedKeyFactory.create("pet_level")
    val xp = plugin.namespacedKeyFactory.create("pet_xp")

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

        if (item.fast().persistentDataContainer.has(xp, PersistentDataType.DOUBLE)) {
            val petXp = item.fast().persistentDataContainer.get(xp, PersistentDataType.DOUBLE)!!
            val petLevel = item.fast().persistentDataContainer.get(level, PersistentDataType.INTEGER)!!

            player.setPetLevel(pet, petLevel)
            player.setPetXP(pet, petXp)

            if (event.hand == EquipmentSlot.HAND) {
                val hand = event.player.inventory.itemInMainHand
                hand.amount = hand.amount - 1
            } else {
                val hand = event.player.inventory.itemInOffHand
                hand.amount = hand.amount - 1
            }
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