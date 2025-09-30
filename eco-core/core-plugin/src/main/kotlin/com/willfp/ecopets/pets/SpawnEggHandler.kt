package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import org.bukkit.entity.Player
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
    @EventHandler(
        ignoreCancelled = true
    )
    fun handle(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) {
            return
        }

        val player = event.player

        val item = event.item ?: return
        val pet = item.petEgg ?: return
        val levelKey = plugin.namespacedKeyFactory.create("${pet.id}_egg_level")
        val level = item.itemMeta.persistentDataContainer.get(levelKey, PersistentDataType.INTEGER)
        val xpKey = plugin.namespacedKeyFactory.create("${pet.id}_egg_xp")
        val xp = item.itemMeta.persistentDataContainer.get(xpKey, PersistentDataType.DOUBLE)

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

        if (level == null) {
            player.setPetLevel(pet, 1)
        } else {
            player.setPetLevel(pet, level)
        }

        if (xp != null) {
            player.setPetXP(pet, xp)
        }
    }
}