package com.willfp.ecopets.pets

import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.formatEco
import com.willfp.ecopets.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
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
import kotlin.math.cos
import kotlin.math.ln1p
import kotlin.math.roundToLong
import kotlin.math.sin

object PetDisplay : Listener {
    private const val DEFAULT_EYE_HEIGHT = 1.62
    private const val MAX_Y_OFFSET = 0.5
    private const val TICKS_PER_SECOND = 20.0
    private const val FULL_CIRCLE = 2 * PI
    private const val ITEM_DISPLAY_YAW_OFFSET = 180f
    private const val PLAYER_HEAD_VISUAL_CENTER_Y = -0.25f

    private var tick = 0L

    private val trackedEntities = mutableMapOf<UUID, PetDisplayEntity>()
    private var settings = DisplaySettings()

    fun reload() {
        val playfulAnimationPath = "pet-entity.item-display.playful-animations"
        val configuredScale = plugin.configYml.getDouble("pet-entity.scale")
        settings = DisplaySettings(
            showHologram = plugin.configYml.getBool("pet-entity.show-hologram"),
            hologramName = plugin.configYml.getString("pet-entity.name"),
            xOffset = plugin.configYml.getDoubleOrNull("pet-entity.location-x-offset")
                ?: plugin.configYml.getDoubleOrNull("pet-entity.location_x_offset")
                ?: 0.75,
            yOffset = plugin.configYml.getDoubleOrNull("pet-entity.location-y-offset") ?: 0.0,
            zOffset = plugin.configYml.getDoubleOrNull("pet-entity.location-z-offset")
                ?: plugin.configYml.getDoubleOrNull("pet-entity.location_z_offset")
                ?: 0.75,
            bobbing = plugin.configYml.getBool("pet-entity.bobbing"),
            bobbingIntensity = plugin.configYml.getDoubleOrNull("pet-entity.bobbing-intensity") ?: 0.15,
            rotation = plugin.configYml.getBool("pet-entity.rotation"),
            rotationIntensity = plugin.configYml.getDoubleOrNull("pet-entity.rotation-intensity") ?: 20.0,
            baseScale = configuredScale.takeIf { it in 0.0625..16.0 } ?: 1.0,
            playfulAnimationsEnabled = plugin.configYml.getBool("$playfulAnimationPath.enabled"),
            playfulAnimationChance = (
                plugin.configYml.getDoubleOrNull("$playfulAnimationPath.chance") ?: 25.0
            ).coerceIn(0.0, 100.0) / 100.0
        )

        // Entity type depends on item-display.enabled, so recreate pets after config reloads.
        shutdown()
    }

    fun tickAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            val chunk = player.chunk
            if (player.isOnline && chunk.isLoaded && chunk.isEntitiesLoaded) {
                tickPlayer(player)
            } else {
                remove(player)
            }
        }

        tick++
    }

    private fun tickPlayer(player: Player) {
        if (player.shouldHidePet) {
            remove(player)
            return
        }

        val entity = getOrNew(player) ?: return
        val pet = player.activePet

        if (pet != null) {
            if (player.isInvisible || player.isDead || !player.isOnline) {
                remove(player)
                return
            }

            val tracked = trackedEntities[player.uniqueId] ?: return
            if (settings.showHologram) {
                if (tracked.lastHologramUpdate < 0 || tick - tracked.lastHologramUpdate >= TICKS_PER_SECOND.toLong()) {
                    @Suppress("DEPRECATION")
                    entity.customName = settings.hologramName
                        .replace("%player%", player.displayName)
                        .replace("%pet%", pet.name)
                        .replace("%level%", player.getPetLevel(pet).toString())
                        .formatEco(player)
                    tracked.lastHologramUpdate = tick
                }
                if (!entity.isCustomNameVisible) {
                    entity.isCustomNameVisible = true
                }
            } else if (entity.isCustomNameVisible) {
                entity.isCustomNameVisible = false
            }

            val isAnimatedItemDisplay = entity is ItemDisplay && settings.playfulAnimationsEnabled
            val targetLocation = getLocation(player, tracked, includeHorizontalOffset = !isAnimatedItemDisplay)
            val location = targetLocation
            val motion = if (isAnimatedItemDisplay) {
                tracked.animation?.getMotion(tick, settings) ?: PetMotion()
            } else {
                val legacyBob = if (settings.bobbing) {
                    NumberUtils.fastSin(tick.toDouble() / (2 * PI) * 0.5) * settings.bobbingIntensity
                } else {
                    0.0
                }
                PetMotion(vertical = legacyBob)
            }

            location.y += settings.yOffset + motion.vertical
            if (isAnimatedItemDisplay) {
                addLocalOffset(location, motion.sideways, motion.forward)
                applyAnimatedTransformation(entity, tracked, motion)
            }

            val desiredYaw = if (settings.rotation && (entity is ItemDisplay || !pet.entityTexture.contains(":"))) {
                if (isAnimatedItemDisplay) {
                    tracked.animation?.getFacingYaw(
                        targetLocation.yaw,
                        tick,
                        motion.yaw
                    ) ?: targetLocation.yaw + ITEM_DISPLAY_YAW_OFFSET
                } else {
                    (settings.rotationIntensity * tick / (2 * PI)).toFloat()
                }
            } else {
                null
            }

            if (desiredYaw != null) {
                location.yaw = desiredYaw
                location.pitch = 0f
            }

            val teleported = location.world != null && tracked.shouldTeleport(location)
            if (teleported) {
                entity.teleport(location)
                desiredYaw?.let(tracked::recordYaw)
            } else if (desiredYaw != null && tracked.shouldRotate(desiredYaw)) {
                entity.setRotation(desiredYaw, 0f)
            }
        }
    }

    private fun getLocation(
        player: Player,
        tracked: PetDisplayEntity? = null,
        includeHorizontalOffset: Boolean = true
    ): Location {
        val playerLocation = player.location

        // Calculate inverted Y-delta from default eye height
        val eyeY = player.eyeLocation.y
        val baseY = playerLocation.y + DEFAULT_EYE_HEIGHT
        var targetYOffset = baseY - eyeY // inverted

        // Clamp to avoid wild swings
        targetYOffset = targetYOffset.coerceIn(-MAX_Y_OFFSET, MAX_Y_OFFSET)

        // Smooth it
        val smoothedYOffset = tracked?.let {
            it.smoothYOffset = lerp(it.smoothYOffset, targetYOffset, 0.15)
            it.smoothYOffset
        } ?: targetYOffset

        // Use base location, not eyeLocation, for static anchoring
        val location = playerLocation.add(0.0, DEFAULT_EYE_HEIGHT + smoothedYOffset, 0.0)
        if (includeHorizontalOffset) {
            addLocalOffset(location, settings.xOffset, -settings.zOffset)
        }
        return location
    }

    private fun addLocalOffset(location: Location, sideways: Double, forwardAmount: Double) {
        if (sideways == 0.0 && forwardAmount == 0.0) {
            return
        }

        val yaw = Math.toRadians(location.yaw.toDouble())
        val sinYaw = sin(yaw)
        val cosYaw = cos(yaw)
        location.add(
            -cosYaw * sideways - sinYaw * forwardAmount,
            0.0,
            -sinYaw * sideways + cosYaw * forwardAmount
        )
    }

    private fun applyAnimatedTransformation(display: ItemDisplay, tracked: PetDisplayEntity, motion: PetMotion) {
        if (
            abs(tracked.appliedScaleXZ - motion.scaleXZ) < 0.001 &&
            abs(tracked.appliedScaleY - motion.scaleY) < 0.001 &&
            abs(tracked.appliedPitch - motion.pitch) < 0.1 &&
            abs(tracked.appliedRoll - motion.roll) < 0.1
        ) {
            return
        }

        val transformation = display.transformation
        transformation.scale.set(
            (settings.baseScale * motion.scaleXZ).toFloat(),
            (settings.baseScale * motion.scaleY).toFloat(),
            (settings.baseScale * motion.scaleXZ).toFloat()
        )
        val rotation = transformation.leftRotation.rotationXYZ(
            Math.toRadians(motion.pitch.toDouble()).toFloat(),
            0f,
            Math.toRadians(motion.roll.toDouble()).toFloat()
        )
        if (!tracked.pet.entityTexture.contains(":")) {
            // Player heads extend downward from the ItemDisplay origin. Keep their visual center fixed
            // while rotating or scaling instead of orbiting around the top-center model pivot.
            val restingCenterY = PLAYER_HEAD_VISUAL_CENTER_Y * settings.baseScale.toFloat()
            val scaledCenterY = PLAYER_HEAD_VISUAL_CENTER_Y * transformation.scale.y
            rotation.transform(0f, scaledCenterY, 0f, transformation.translation)
            transformation.translation.set(
                -transformation.translation.x,
                restingCenterY - transformation.translation.y,
                -transformation.translation.z
            )
        } else {
            transformation.translation.zero()
        }
        display.transformation = transformation
        tracked.appliedScaleXZ = motion.scaleXZ
        tracked.appliedScaleY = motion.scaleY
        tracked.appliedPitch = motion.pitch
        tracked.appliedRoll = motion.roll
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

            val location = getLocation(player)
            val entity = pet.makePetEntity().spawn(location)

            trackedEntities[player.uniqueId] = PetDisplayEntity(
                entity,
                pet,
                getTargetYOffset(player),
                if (entity is ItemDisplay) {
                    PetAnimation(player.uniqueId, settings.playfulAnimationChance)
                } else {
                    null
                }
            )
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
        val iterator = trackedEntities.iterator()
        while (iterator.hasNext()) {
            val tracked = iterator.next().value
            if (event.chunk == tracked.entity.chunk) {
                tracked.entity.remove()
                iterator.remove()
            }
        }
    }

    private class PetDisplayEntity(
        val entity: Entity,
        val pet: Pet,
        var smoothYOffset: Double,
        val animation: PetAnimation?,
        var lastHologramUpdate: Long = -1L,
        var appliedScaleXZ: Double = 1.0,
        var appliedScaleY: Double = 1.0,
        var appliedPitch: Float = 0f,
        var appliedRoll: Float = 0f,
        private var lastSentX: Double = Double.NaN,
        private var lastSentY: Double = Double.NaN,
        private var lastSentZ: Double = Double.NaN,
        private var lastSentYaw: Float = Float.NaN
    ) {
        fun shouldTeleport(location: Location): Boolean {
            val changed = lastSentX.isNaN() ||
                squaredDistance(lastSentX, lastSentY, lastSentZ, location.x, location.y, location.z) > 0.000025
            if (changed) {
                lastSentX = location.x
                lastSentY = location.y
                lastSentZ = location.z
            }
            return changed
        }

        fun shouldRotate(yaw: Float): Boolean {
            if (lastSentYaw.isNaN() || abs(shortestAngle(lastSentYaw, yaw)) > 0.15f) {
                lastSentYaw = yaw
                return true
            }
            return false
        }

        fun recordYaw(yaw: Float) {
            lastSentYaw = yaw
        }

        private fun squaredDistance(
            x1: Double,
            y1: Double,
            z1: Double,
            x2: Double,
            y2: Double,
            z2: Double
        ): Double {
            val x = x2 - x1
            val y = y2 - y1
            val z = z2 - z1
            return x * x + y * y + z * z
        }

        private fun shortestAngle(from: Float, to: Float): Float =
            ((to - from + 540f) % 360f) - 180f
    }

    // Animation tuning is intentionally hardcoded; only enabled and chance are user-configurable.
    private class PetAnimation(
        uuid: UUID,
        private val chancePerSecond: Double
    ) {
        private val random = Random(uuid.mostSignificantBits xor uuid.leastSignificantBits)
        private var action = PetAction.IDLE
        private var actionStartedAt = 0L
        private var actionDuration = 1L
        private var nextActionAt = Long.MIN_VALUE
        private var side = 1.0
        private var sideFrom = 1.0
        private var sideTo = 1.0
        private var lookDirection = 1.0
        private var idleStartedAt = Long.MIN_VALUE
        private var previousIdleBob = 0.0
        private var facingYaw = Float.NaN
        private var idleFacingOffset = 0.0
        private var nextFacingChangeAt = Long.MIN_VALUE
        private var lastAction = PetAction.IDLE
        private var actionStrength = 1.0
        private var repositionRadius = 0.0
        private var repositionAngleFrom = 0.0
        private var repositionAngleTo = 0.0

        fun getMotion(currentTick: Long, displaySettings: DisplaySettings): PetMotion {
            if (action == PetAction.IDLE) {
                val idleBob = getIdleBob(currentTick, displaySettings)
                if (nextActionAt == Long.MIN_VALUE) {
                    scheduleNextAction(currentTick)
                }
                val crossedNeutral = idleBob == 0.0 || previousIdleBob == 0.0 || idleBob * previousIdleBob < 0.0
                previousIdleBob = idleBob

                if (currentTick >= nextActionAt && crossedNeutral) {
                    startAction(currentTick, displaySettings)
                } else {
                    return idleMotion(idleBob, displaySettings)
                }
            } else if (currentTick - actionStartedAt >= actionDuration) {
                finishAction()
                action = PetAction.IDLE
                idleStartedAt = currentTick
                previousIdleBob = 0.0
                scheduleNextAction(currentTick)
                return idleMotion(0.0, displaySettings)
            }

            val progress = ((currentTick - actionStartedAt).toDouble() / actionDuration).coerceIn(0.0, 1.0)
            return action.motion(this, progress, displaySettings)
        }

        private fun swayMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val envelope = NumberUtils.fastSin(PI * progress)
            val wave = NumberUtils.fastSin(FULL_CIRCLE * progress) * envelope
            return settings.motion(
                sidewaysOffset = wave * 0.14 * actionStrength,
                forwardOffset = (fastCos(FULL_CIRCLE * progress) - 1.0) *
                    envelope * 0.05 * actionStrength,
                yaw = (wave * 25.0 * 0.18 * actionStrength).toFloat(),
                pitch = (NumberUtils.fastSin(FULL_CIRCLE * 2 * progress) *
                    envelope * 3.0 * actionStrength).toFloat(),
                roll = (-wave * 9.0 * actionStrength).toFloat()
            )
        }

        private fun puffMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val puff = when {
                progress < 0.25 -> smootherStep(progress / 0.25)
                progress < 0.62 -> 1.0
                else -> {
                    val settle = (progress - 0.62) / 0.38
                    (1.0 - smootherStep(settle)) +
                        NumberUtils.fastSin(PI * 3 * settle) * (1.0 - settle) * 0.08
                }
            }
            return settings.motion(
                pitch = (NumberUtils.fastSin(FULL_CIRCLE * 2 * progress) *
                    (1.0 - progress) * 1.5).toFloat(),
                roll = (NumberUtils.fastSin(FULL_CIRCLE * 3 * progress) *
                    (1.0 - progress) * 2.0).toFloat(),
                scaleXZ = 1.0 + puff * 0.10 * actionStrength,
                scaleY = 1.0 + puff * 0.10 * 0.85 * actionStrength
            )
        }

        private fun orbitMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val envelopeSin = NumberUtils.fastSin(PI * progress)
            val envelope = envelopeSin * envelopeSin
            val angle = lookDirection * FULL_CIRCLE * progress
            return settings.motion(
                vertical = NumberUtils.fastSin(angle * 2) *
                    envelope * 0.035 * 0.35 * actionStrength,
                sidewaysOffset = NumberUtils.fastSin(angle) *
                    envelope * 0.16 * actionStrength,
                forwardOffset = (fastCos(angle) - 1.0) *
                    envelope * 0.16 * 0.55 * actionStrength,
                yaw = (NumberUtils.fastSin(angle) * envelope *
                    25.0 * 0.18 * actionStrength).toFloat(),
                pitch = (NumberUtils.fastSin(angle) * envelope *
                    8.0 * 0.25 * actionStrength).toFloat(),
                roll = (-fastCos(angle) * envelope *
                    8.0 * lookDirection * actionStrength).toFloat()
            )
        }

        private fun lookAroundMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val lookYaw = getCuriousLookYaw(progress)
            val envelope = NumberUtils.fastSin(PI * progress)
            return settings.motion(
                yaw = lookYaw.toFloat(),
                pitch = (NumberUtils.fastSin(FULL_CIRCLE * progress) * envelope * 3.0).toFloat(),
                roll = (-lookYaw * 0.10).toFloat()
            )
        }

        private fun backflipMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val arc = NumberUtils.fastSin(PI * progress)
            val contact = when {
                progress < 0.16 -> NumberUtils.fastSin(PI * progress / 0.16)
                progress > 0.84 -> NumberUtils.fastSin(PI * (progress - 0.84) / 0.16)
                else -> 0.0
            }
            return settings.motion(
                vertical = arc * 0.10 * actionStrength,
                forwardOffset = -arc * 0.04 * actionStrength,
                pitch = (-360.0 * smootherStep(progress) * lookDirection).toFloat(),
                roll = (NumberUtils.fastSin(FULL_CIRCLE * progress) * 2.0).toFloat(),
                scaleXZ = 1.0 + contact * 0.10 * 0.5,
                scaleY = 1.0 - contact * 0.10 + arc * 0.025
            )
        }

        private fun pirouetteMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val envelope = NumberUtils.fastSin(PI * progress)
            return settings.motion(
                yaw = (360.0 * smootherStep(progress) * lookDirection).toFloat(),
                pitch = (envelope * 2.0).toFloat(),
                roll = (-envelope * lookDirection * 4.0).toFloat(),
                scaleXZ = 1.0 + envelope * 0.025,
                scaleY = 1.0 - envelope * 0.035
            )
        }

        private fun barrelRollMotion(progress: Double, settings: DisplaySettings): PetMotion {
            val envelope = NumberUtils.fastSin(PI * progress)
            return settings.motion(
                vertical = NumberUtils.fastSin(FULL_CIRCLE * progress) * 0.015,
                yaw = (NumberUtils.fastSin(FULL_CIRCLE * progress) * 4.0).toFloat(),
                pitch = (envelope * 3.0).toFloat(),
                roll = (360.0 * smootherStep(progress) * lookDirection).toFloat()
            )
        }

        fun getFacingYaw(
            playerYaw: Float,
            currentTick: Long,
            actionYaw: Float
        ): Float {
            if (nextFacingChangeAt == Long.MIN_VALUE || currentTick >= nextFacingChangeAt) {
                idleFacingOffset = (random.nextDouble() * 2.0 - 1.0) * 32.0
                nextFacingChangeAt = currentTick + randomDelay(60L, 140L)
            }

            val target = playerYaw + ITEM_DISPLAY_YAW_OFFSET + idleFacingOffset.toFloat()
            if (facingYaw.isNaN()) {
                facingYaw = target
                return facingYaw + actionYaw
            }

            facingYaw += shortestAngle(facingYaw, target) * 0.12f
            return facingYaw + actionYaw
        }

        private fun getIdleBob(
            currentTick: Long,
            displaySettings: DisplaySettings
        ): Double {
            if (!displaySettings.bobbing) {
                return 0.0
            }

            if (idleStartedAt == Long.MIN_VALUE) {
                val randomPhase = random.nextInt(64)
                idleStartedAt = currentTick - randomPhase
            }

            return NumberUtils.fastSin(
                (currentTick - idleStartedAt).toDouble() / 64.0 * FULL_CIRCLE
            ) * displaySettings.bobbingIntensity
        }

        private fun idleMotion(
            idleBob: Double,
            displaySettings: DisplaySettings
        ): PetMotion = displaySettings.motion(vertical = idleBob)

        private fun DisplaySettings.motion(
            vertical: Double = 0.0,
            sidewaysOffset: Double = 0.0,
            forwardOffset: Double = 0.0,
            yaw: Float = 0f,
            pitch: Float = 0f,
            roll: Float = 0f,
            scaleXZ: Double = 1.0,
            scaleY: Double = 1.0
        ): PetMotion = PetMotion(
            vertical = vertical,
            sideways = side * xOffset + sidewaysOffset,
            forward = -zOffset + forwardOffset,
            yaw = yaw,
            pitch = pitch,
            roll = roll,
            scaleXZ = scaleXZ,
            scaleY = scaleY
        )

        private fun repositionMotion(
            progress: Double,
            displaySettings: DisplaySettings
        ): PetMotion {
            val angle = lerp(repositionAngleFrom, repositionAngleTo, smootherStep(progress))
            val desiredSide = sin(angle) * repositionRadius
            val desiredForward = -cos(angle) * repositionRadius
            val envelope = NumberUtils.fastSin(PI * progress)

            return PetMotion(
                sideways = desiredSide,
                forward = desiredForward,
                yaw = (-sideFrom * envelope * 25.0 * 0.28 * actionStrength).toFloat(),
                pitch = (NumberUtils.fastSin(FULL_CIRCLE * progress) *
                    envelope * 2.5 * actionStrength).toFloat(),
                roll = (-sideFrom * envelope *
                    9.0 * actionStrength).toFloat(),
                scaleXZ = 1.0,
                scaleY = 1.0
            )
        }

        private fun getCuriousLookYaw(progress: Double): Double {
            val fullLook = lookDirection * 25.0
            return when {
                progress < 0.20 -> fullLook * smoothStep(progress / 0.20)
                progress < 0.58 -> fullLook
                progress < 0.76 -> lerp(fullLook, -fullLook * 0.4, smoothStep((progress - 0.58) / 0.18))
                progress < 0.86 -> -fullLook * 0.4
                else -> lerp(-fullLook * 0.4, 0.0, smoothStep((progress - 0.86) / 0.14))
            }
        }

        private fun startAction(
            currentTick: Long,
            displaySettings: DisplaySettings
        ) {
            fun pickAction(): PetAction {
                val roll = random.nextInt(13)
                return when {
                    roll < 3 -> PetAction.SWAY
                    roll < 5 -> PetAction.PUFF
                    roll < 7 -> PetAction.ORBIT
                    roll == 7 && displaySettings.rotation -> PetAction.LOOK_AROUND
                    roll < 10 -> PetAction.REPOSITION
                    roll == 10 -> PetAction.BACKFLIP
                    roll == 11 && displaySettings.rotation -> PetAction.PIROUETTE
                    else -> PetAction.BARREL_ROLL
                }
            }

            action = pickAction()
            if (action == lastAction) {
                action = pickAction()
            }
            lastAction = action
            actionStrength = 0.85 + random.nextDouble() * 0.30
            actionStartedAt = currentTick
            actionDuration = (action.durationTicks * (0.90 + random.nextDouble() * 0.20))
                .roundToLong()
                .coerceAtLeast(1L)

            lookDirection = if (side > 0.0) -1.0 else 1.0
            if (action == PetAction.REPOSITION) {
                sideFrom = side
                sideTo = -side
                repositionRadius = kotlin.math.hypot(displaySettings.xOffset, displaySettings.zOffset)
                // Signed z preserves custom offsets; opposite side angles interpolate behind the player.
                val sideAngle = kotlin.math.atan2(displaySettings.xOffset, displaySettings.zOffset)
                repositionAngleFrom = sideFrom * sideAngle
                repositionAngleTo = if (displaySettings.xOffset == 0.0) {
                    repositionAngleFrom
                } else {
                    sideTo * sideAngle
                }
            }
        }

        private fun finishAction() {
            if (action == PetAction.REPOSITION) {
                side = sideTo
            }
        }

        private fun scheduleNextAction(currentTick: Long) {
            if (chancePerSecond <= 0.0) {
                nextActionAt = Long.MAX_VALUE
                return
            }

            val secondsUntilAction = if (chancePerSecond >= 1.0) {
                1L
            } else {
                val failedSeconds = (
                    ln1p(-random.nextDouble()) / ln1p(-chancePerSecond)
                ).toLong().coerceAtMost(Int.MAX_VALUE.toLong() - 1L)
                failedSeconds + 1L
            }
            nextActionAt = currentTick + secondsUntilAction * TICKS_PER_SECOND.toLong()
        }

        private fun smoothStep(value: Double): Double = value * value * (3 - 2 * value)

        private fun smootherStep(value: Double): Double =
            value * value * value * (value * (value * 6 - 15) + 10)

        private fun fastCos(value: Double): Double = NumberUtils.fastSin(value + PI / 2)

        private fun randomDelay(minimum: Long, configuredMaximum: Long): Long {
            val maximum = configuredMaximum.coerceAtLeast(minimum)
            val range = (maximum - minimum + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            return minimum + random.nextInt(range)
        }

        private fun shortestAngle(from: Float, to: Float): Float =
            ((to - from + 540f) % 360f) - 180f

        private fun interface AnimationMotion {
            operator fun invoke(
                state: PetAnimation,
                progress: Double,
                settings: DisplaySettings
            ): PetMotion
        }

        private enum class PetAction(
            val durationTicks: Long,
            val motion: AnimationMotion
        ) {
            IDLE(1L, PetAnimation::idleMotion),
            SWAY(40L, PetAnimation::swayMotion),
            PUFF(32L, PetAnimation::puffMotion),
            ORBIT(44L, PetAnimation::orbitMotion),
            LOOK_AROUND(44L, PetAnimation::lookAroundMotion),
            BACKFLIP(25L, PetAnimation::backflipMotion),
            PIROUETTE(22L, PetAnimation::pirouetteMotion),
            BARREL_ROLL(23L, PetAnimation::barrelRollMotion),
            REPOSITION(44L, PetAnimation::repositionMotion)
        }
    }

    private data class PetMotion(
        val vertical: Double = 0.0,
        val sideways: Double = 0.0,
        val forward: Double = 0.0,
        val yaw: Float = 0f,
        val pitch: Float = 0f,
        val roll: Float = 0f,
        val scaleXZ: Double = 1.0,
        val scaleY: Double = 1.0
    )

    private data class DisplaySettings(
        val showHologram: Boolean = true,
        val hologramName: String = "%player%&f's %pet%&f (Lvl. %level%)",
        val xOffset: Double = 0.75,
        val yOffset: Double = 0.0,
        val zOffset: Double = 0.75,
        val bobbing: Boolean = true,
        val bobbingIntensity: Double = 0.15,
        val rotation: Boolean = true,
        val rotationIntensity: Double = 20.0,
        val baseScale: Double = 1.0,
        val playfulAnimationsEnabled: Boolean = false,
        val playfulAnimationChance: Double = 0.25
    )

    private fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }

    private fun getTargetYOffset(player: Player): Double {
        val eyeY = player.eyeLocation.y
        val baseY = player.location.y + DEFAULT_EYE_HEIGHT
        return (baseY - eyeY).coerceIn(-MAX_Y_OFFSET, MAX_Y_OFFSET)
    }

}
