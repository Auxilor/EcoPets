package com.willfp.ecopets.pets

import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.plugin
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

object SpawnEggHandler : Listener {
    private val PlayerInteractEvent.isUnblockedRightClick: Boolean
        get() = (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) &&
            useItemInHand() != Event.Result.DENY

    @EventHandler
    fun handle(event: PlayerInteractEvent) {
        val item = event.item
        val pet = item?.petEgg

        if (item == null || pet == null || !event.isUnblockedRightClick) {
            return
        }

        val player = event.player

        event.isCancelled = true
        event.setUseItemInHand(Event.Result.DENY)

        val eggLevel = item.petEggLevel
        val eggXp = item.petEggXp

        if (!player.canRedeem(pet, eggLevel, eggXp)) {
            return
        }

        if (event.hand == EquipmentSlot.HAND) {
            event.player.inventory.itemInMainHand.amount -= 1
        } else {
            event.player.inventory.itemInOffHand.amount -= 1
        }

        player.setPetLevel(pet, eggLevel.coerceIn(minimumValue = 1, maximumValue = pet.maxLevel))
        player.setPetXP(pet, eggXp)
        player.sendMessage(
            plugin.langYml.getMessage("pet-spawned", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%pet%", pet.name)
        )
    }

    private fun Player.canRedeem(pet: Pet, eggLevel: Int, eggXp: Double): Boolean {
        val ownedLevel = this.getPetLevel(pet)
        val policy = plugin.configYml.getString("pet-egg.redeem-conflict").lowercase()
        val isUpgrade = eggLevel > ownedLevel || eggLevel == ownedLevel && eggXp > this.getPetXP(pet)
        val canRedeem = ownedLevel <= 0 || policy != "reject" && isUpgrade

        if (!canRedeem) {
            this.sendMessage(
                if (policy == "reject") plugin.langYml.getMessage("cannot-spawn-pet")
                else plugin.langYml.getMessage("egg-lower-level")
            )
        }

        return canRedeem
    }
}
