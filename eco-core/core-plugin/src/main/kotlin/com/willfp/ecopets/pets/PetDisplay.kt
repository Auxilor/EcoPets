package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.formatEco
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

class PetDisplay(
    private val plugin: EcoPlugin
) : Listener {
    private var tick = 0

    private val trackedEntities = mutableMapOf<UUID, ArmorStand>()

    fun tickAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            tickPlayer(player)
        }

        tick++
    }

    private fun tickPlayer(player: Player) {
        val stand = getOrNew(player) ?: return
        val pet = player.activePet

        if (pet != null) {
            stand.customName = plugin.configYml.getString("pet-entity.name")
                .replace("%player%", player.displayName)
                .replace("%pet%", pet.name)
                .replace("%level%", player.getPetLevel(pet).toString())
                .formatEco(player)

            val location = getLocation(player)

            location.y += NumberUtils.fastSin(tick / (2 * PI) * 0.5) * 0.15

            stand.teleport(location)

            if (!pet.entityTexture.contains(":")) {
                stand.setRotation((20 * tick / (2 * PI)).toFloat(), 0f)
            }
        }
    }

    private fun getLocation(player: Player): Location {
        val offset = player.eyeLocation.direction.clone().normalize()
            .multiply(-1)
            .apply { y = abs(y) }
            .rotateAroundY(PI / 6)

        return player.eyeLocation.clone().add(offset)
    }

    private fun getOrNew(player: Player): ArmorStand? {
        val existing = trackedEntities[player.uniqueId]
        val pet = player.activePet

        if (existing == null || existing.isDead || pet == null) {
            existing?.remove()
            trackedEntities.remove(player.uniqueId)

            if (pet == null) {
                return null
            }

            val location = getLocation(player)
            val stand = pet.makePetEntity().spawn(location)

            trackedEntities[player.uniqueId] = stand
        }

        return trackedEntities[player.uniqueId]
    }

    fun shutdown() {
        for (stand in trackedEntities.values) {
            stand.remove()
        }

        trackedEntities.clear()
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        trackedEntities[event.player.uniqueId]?.remove()
        trackedEntities.remove(event.player.uniqueId)
    }
}
