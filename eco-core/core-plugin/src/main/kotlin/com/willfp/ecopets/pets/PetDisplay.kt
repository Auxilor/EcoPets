package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.formatEco
import com.willfp.libreforge.getDoubleFromExpression
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

    private val smoothYOffsetMap = mutableMapOf<UUID, Double>()
    private val lastUpdateMap = mutableMapOf<UUID, Long>()
    private val expireAfterMillis = 5_000L
    companion object {
        private const val DEFAULT_EYE_HEIGHT = 1.62
        private const val MAX_Y_OFFSET = 0.5
    }

    private fun tickPlayer(player: Player) {
        if (player.shouldHidePet) {
            remove(player)
            return
        }

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
            val offset = plugin.configYml.getDoubleOrNull("pet-entity.location-y-offset") ?: 0.0
            val bobbing = plugin.configYml.getDoubleOrNull("pet-entity.bobbing-intensity") ?: 0.15


            if (plugin.configYml.getBool("pet-entity.bobbing")) {
                location.y += offset + NumberUtils.fastSin(tick / (2 * PI) * 0.5) * bobbing
            } else {
                location.y += offset
            }

            if (location.world != null) {
                stand.teleport(location)
            }

            if (!pet.entityTexture.contains(":") && plugin.configYml.getBool("pet-entity.rotation")) {
                val intensity = plugin.configYml.getDoubleOrNull("pet-entity.rotation-intensity") ?: 20.0
                stand.setRotation((intensity * tick / (2 * PI)).toFloat(), 0f)
            }
        }
    }

    private fun getLocation(player: Player): Location {
        val direction = player.eyeLocation.direction.clone().normalize()

        val locationXZOffset = plugin.configYml.getDoubleOrNull("pet-entity.location_xz_offset") ?: 0.75
        val offset = direction.clone().multiply(-locationXZOffset)

        val uuid = player.uniqueId
        val currentTime = System.currentTimeMillis()

        // Calculate inverted Y-delta from default eye height
        val eyeY = player.eyeLocation.y
        val baseY = player.location.y + DEFAULT_EYE_HEIGHT
        var targetYOffset = baseY - eyeY // inverted

        // Clamp to avoid wild swings
        targetYOffset = targetYOffset.coerceIn(-MAX_Y_OFFSET, MAX_Y_OFFSET)

        // Smooth it
        val lastY = smoothYOffsetMap[uuid] ?: targetYOffset
        val lastUpdate = lastUpdateMap[uuid] ?: currentTime
        val deltaTime = currentTime - lastUpdate

        val t = when {
            deltaTime > 1000 -> 0.5
            deltaTime > 500 -> 0.3
            else -> 0.15
        }

        val smoothedYOffset = lerp(lastY, targetYOffset, t)
        smoothYOffsetMap[uuid] = smoothedYOffset
        lastUpdateMap[uuid] = currentTime

        offset.y = smoothedYOffset

        // Smooth X/Z bias
        if (abs(offset.x) < 0.3) offset.x *= 1.5
        if (abs(offset.z) < 0.3) offset.z *= 1.5

        offset.rotateAroundY(Math.PI / 12)
        cleanupOldEntries(currentTime)

        // Use base location, not eyeLocation, for static anchoring
        return player.location.clone().add(0.0, DEFAULT_EYE_HEIGHT, 0.0).add(offset)
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

    private fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }

    private fun cleanupOldEntries(currentTime: Long) {
        val expired = lastUpdateMap.filterValues { currentTime - it > expireAfterMillis }.keys
        for (uuid in expired) {
            smoothYOffsetMap.remove(uuid)
            lastUpdateMap.remove(uuid)
        }
    }
}
