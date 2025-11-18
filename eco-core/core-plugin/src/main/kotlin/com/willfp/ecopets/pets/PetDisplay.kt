package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.formatEco
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

class PetDisplay(
    private val plugin: EcoPlugin
) : Listener {
    private var tick = 0

    private val trackedEntities = mutableMapOf<UUID, PetDisplayEntity>()

    fun tickAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOnline && player.location.chunk.isLoaded && player.location.chunk.isEntitiesLoaded) {
                tickPlayer(player)
            } else {
                remove(player)
            }
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

        val entity = getOrNew(player) ?: return
        val pet = player.activePet
        val showHologram = plugin.configYml.getBool("pet-entity.show-hologram")

        if (pet != null) {
            if (player.isInvisible || player.isDead || !player.isOnline) {
                remove(player)
                return
            }

            if (showHologram) {
                @Suppress("DEPRECATION")
                entity.customName = plugin.configYml.getString("pet-entity.name")
                    .replace("%player%", player.displayName)
                    .replace("%pet%", pet.name)
                    .replace("%level%", player.getPetLevel(pet).toString())
                    .formatEco(player)
                entity.isCustomNameVisible = true
            } else {
                entity.isCustomNameVisible = false
            }

            val location = getLocation(player, if ((entity is ArmorStand)) 0.0 else 1.0)
            val offset = plugin.configYml.getDoubleOrNull("pet-entity.location-y-offset") ?: 0.0
            val bobbing = plugin.configYml.getDoubleOrNull("pet-entity.bobbing-intensity") ?: 0.15



            if (plugin.configYml.getBool("pet-entity.bobbing")) {
                location.y += offset + NumberUtils.fastSin(tick / (2 * PI) * 0.5) * bobbing
            } else {
                location.y += offset
            }

            if (location.world != null) {
                entity.teleport(location)
            }

            if (!pet.entityTexture.contains(":") && plugin.configYml.getBool("pet-entity.rotation")) {
                val intensity = plugin.configYml.getDoubleOrNull("pet-entity.rotation-intensity") ?: 20.0
                entity.setRotation((intensity * tick / (2 * PI)).toFloat(), 0f)
            }
        }
    }

    private fun getLocation(player: Player, d: Double): Location {
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

    private fun getOrNew(player: Player): Entity? {
        val tracked = trackedEntities[player.uniqueId]
        val existing = tracked?.entity

        val pet = player.activePet
        if (pet != tracked?.pet) {
            tracked?.entity?.remove()
        }

        if (existing == null || existing.isDead || pet == null) {
            existing?.remove()
            trackedEntities.remove(player.uniqueId)

            if (pet == null) {
                return null
            }

            val location = getLocation(player, 0.0)
            val entity = pet.makePetEntity().spawn(location)

            trackedEntities[player.uniqueId] = PetDisplayEntity(entity, pet)
        }

        return trackedEntities[player.uniqueId]?.entity
    }

    fun shutdown() {
        for (stand in trackedEntities.values) {
            stand.entity.remove()
        }

        trackedEntities.clear()
    }

    private fun remove(player: Player) {
        trackedEntities[player.uniqueId]?.entity?.remove()
        trackedEntities.remove(player.uniqueId)
    }

    private fun remove(uuid: UUID) {
        trackedEntities[uuid]?.entity?.remove()
        trackedEntities.remove(uuid)
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

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        remove(event.player)
    }

    @EventHandler
    fun onEntitiesUnload(event: EntitiesUnloadEvent) {
        trackedEntities.entries.forEach {
            if (event.chunk == it.value.entity.chunk) {
                remove(it.key)
            }
        }
    }

    private data class PetDisplayEntity(
        val entity: Entity,
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
