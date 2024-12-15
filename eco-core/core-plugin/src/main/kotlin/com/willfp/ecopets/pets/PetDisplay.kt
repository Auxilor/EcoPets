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
import org.bukkit.event.world.ChunkUnloadEvent
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

    private fun tickPlayer(player: Player) {
        val entity = getOrNew(player) ?: return
        val pet = player.activePet

        if (pet != null) {
            if (player.isInvisible || player.isDead || !player.isOnline) {
                remove(player)
                return
            }

            @Suppress("DEPRECATION")
            entity.customName = plugin.configYml.getString("pet-entity.name")
                .replace("%player%", player.displayName)
                .replace("%pet%", pet.name)
                .replace("%level%", player.getPetLevel(pet).toString())
                .formatEco(player)

            val location = getLocation(player, if ((entity is ArmorStand)) 0.0 else 1.0)

            location.y += NumberUtils.fastSin(tick / (2 * PI) * 0.5) * 0.15

            if (location.world != null) {
                entity.teleport(location)
            }

            if (!pet.entityTexture.contains(":")) {
                entity.setRotation((20 * tick / (2 * PI)).toFloat(), 0f)
            }
        }
    }

    private fun getLocation(player: Player, yOffset: Double): Location {
        val offset = player.eyeLocation.direction.clone().normalize()
            .multiply(-1)
            .apply {
                y = abs(y) + yOffset

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
}
