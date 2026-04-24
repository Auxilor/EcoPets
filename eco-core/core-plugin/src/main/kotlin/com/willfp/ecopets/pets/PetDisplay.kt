package com.willfp.ecopets.pets

import com.willfp.eco.core.Prerequisite
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.formatEco
import com.willfp.ecopets.plugin
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

object PetDisplay : Listener {
    private const val DEFAULT_EYE_HEIGHT = 1.62
    private const val MAX_Y_OFFSET = 0.5

    private var tick = 0

    private val trackedEntities = mutableMapOf<UUID, PetDisplayEntity>()

    fun tickAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.scheduler.runTask(player) { // folia issue
                if (player.isOnline && player.location.chunk.isLoaded && player.location.chunk.isEntitiesLoaded) {
                    tickPlayer(player)
                } else {
                    remove(player)
                }
            }
        }

        tick++
    }

    private val smoothYOffsetMap = mutableMapOf<UUID, Double>()
    private val lastUpdateMap = mutableMapOf<UUID, Long>()
    private val expireAfterMillis = 5_000L

    private fun tickPlayer(player: Player) {
        if (player.shouldHidePet || player.isInvisible || player.isDead || !player.isOnline) {
            remove(player)
            return
        }

        val entity = getOrNew(player) ?: return
        plugin.scheduler.runTask(entity) { // folia issue
            val pet = player.activePet
            val showHologram = plugin.configYml.getBool("pet-entity.show-hologram")

            if (pet != null) {
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

                // makes the pet follow the player's sneaking state (so the name can be hidden when sneaking)
                entity.isSneaking = player.isSneaking

                val location = getLocation(player, if ((entity is ArmorStand)) 0.0 else 1.0)
                val offset = plugin.configYml.getDoubleOrNull("pet-entity.location-y-offset") ?: 0.0
                val bobbing = plugin.configYml.getDoubleOrNull("pet-entity.bobbing-intensity") ?: 0.15

                val yOnPlayerSneaking = when {
                    player.isSneaking -> -0.3
                    else -> 0.0
                }

                // make the pet not be in the player's view when looking up or down
                val yOnPlayerLookingUp = when {
                    player.pitch < -75 -> -1.5
                    player.pitch > 75 -> 0.5
                    else -> 0.0
                }

                if (plugin.configYml.getBool("pet-entity.bobbing")) {
                    location.y += yOnPlayerSneaking + yOnPlayerLookingUp + offset + NumberUtils.fastSin(tick / (2 * PI) * 0.5) * bobbing
                } else {
                    location.y += yOnPlayerSneaking + yOnPlayerLookingUp + offset
                }

                if (!pet.entityTexture.contains(":") && plugin.configYml.getBool("pet-entity.rotation")) {
                    val intensity = plugin.configYml.getDoubleOrNull("pet-entity.rotation-intensity") ?: 20.0
                    location.yaw = (intensity * tick / (2 * PI)).toFloat()
                    location.pitch = 0f
                }

                if (location.world != null) {
                    if (Prerequisite.HAS_PAPER.isMet)
                        entity.teleportAsync(location)
                    else
                        entity.teleport(location) // damn spigot!
                }
            }
        }
    }

    private fun getLocation(player: Player, d: Double): Location {
        val direction = player.eyeLocation.direction.clone().normalize()

        val locationXOffset = plugin.configYml.getDoubleOrNull("pet-entity.location_x_offset") ?: 0.75
        val locationZOffset = plugin.configYml.getDoubleOrNull("pet-entity.location_z_offset") ?: 0.75
        val offset = direction.clone().apply {
            x *= -locationXOffset
            z *= -locationZOffset
        }

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

    fun get(player: Player): Entity? {
        val tracked = trackedEntities[player.uniqueId]
        val existing = tracked?.entity
        return existing
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
                .apply { isPersistent = false }

            trackedEntities[player.uniqueId] = PetDisplayEntity(entity, pet)
        }

        return trackedEntities[player.uniqueId]?.entity
    }

    private fun remove(player: Player) {
        this.remove(player.uniqueId)
    }

    private fun remove(uuid: UUID) {
        trackedEntities[uuid]?.entity?.let {
            plugin.scheduler.runTask(it) {
                it.remove()
            }
        }
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
            plugin.scheduler.runTask(it.value.entity) { // folia issue
                if (event.chunk == it.value.entity.chunk) {
                    remove(it.key)
                }
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
