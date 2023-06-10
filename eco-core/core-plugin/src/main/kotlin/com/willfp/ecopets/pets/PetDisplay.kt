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
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs

class PetDisplay(
    private val plugin: EcoPlugin
) : Listener {
    private var tick = 0

    private val trackedEntities = mutableMapOf<UUID, PetArmorStand>()

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
            if (player.isInvisible) {
                remove(player)
                return
            }

            @Suppress("DEPRECATION")
            stand.customName = plugin.configYml.getString("pet-entity.name")
                .replace("%player%", player.displayName)
                .replace("%pet%", pet.name)
                .replace("%level%", player.getPetLevel(pet).toString())
                .formatEco(player)

            val location = getLocation(player)

            location.y += NumberUtils.fastSin(tick / (2 * PI) * 0.5) * 0.15

            if (location.world != null) {
                stand.teleport(location)
            }

            if (!pet.entityTexture.contains(":")) {
                stand.setRotation((20 * tick / (2 * PI)).toFloat(), 0f)
            }
        }
    }

    private fun getLocation(player: Player): Location {
        val offset = player.eyeLocation.direction.clone().normalize()
            .multiply(-1)
            .apply {
                y = abs(y)

                if (abs(x) < 0.5) {
                    x = 0.5
                }

                if (abs(z) < 0.5) {
                    z = 0.5
                }
            }
            .rotateAroundY(PI / 6)

        return player.eyeLocation.clone().add(offset)
    }

    private fun getOrNew(player: Player): ArmorStand? {
        if (player.isInvisible) {
            return null
        }

        val tracked = trackedEntities[player.uniqueId]
        val existing = tracked?.stand

        val pet = player.activePet
        if (pet != tracked?.pet) {
            tracked?.stand?.remove()
        }

        if (existing == null || existing.isDead || pet == null) {
            existing?.remove()
            trackedEntities.remove(player.uniqueId)

            if (pet == null) {
                return null
            }

            val location = getLocation(player)
            val stand = pet.makePetEntity().spawn(location)

            trackedEntities[player.uniqueId] = PetArmorStand(stand, pet)
        }

        return trackedEntities[player.uniqueId]?.stand
    }

    fun shutdown() {
        for (stand in trackedEntities.values) {
            stand.stand.remove()
        }

        trackedEntities.clear()
    }

    private fun remove(player: Player) {
        trackedEntities[player.uniqueId]?.stand?.remove()
        trackedEntities.remove(player.uniqueId)
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        remove(event.player)
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        remove(event.player)
    }

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        remove(event.player)
    }

    private data class PetArmorStand(
        val stand: ArmorStand,
        val pet: Pet
    )
}
