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
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln1p
import kotlin.math.roundToLong

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
            animation = AnimationSettings(
                enabled = plugin.configYml.getBool("$playfulAnimationPath.enabled"),
                chancePerSecond = (
                    plugin.configYml.getDoubleOrNull("$playfulAnimationPath.chance") ?: 25.0
                ).coerceIn(0.0, 100.0) / 100.0
            )
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

            val isAnimatedItemDisplay = entity is ItemDisplay && settings.animation.enabled
            val targetLocation = getLocation(player, tracked, includeHorizontalOffset = !isAnimatedItemDisplay)
            val location = targetLocation
            val motion = if (isAnimatedItemDisplay) {
                tracked.animation?.getMotion(
                    tick,
                    settings.animation,
                    settings
                ) ?: PetMotion()
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
                addLocalOffset(location, player, motion.sideways, motion.forward)
                applyAnimatedTransformation(entity, tracked, motion)
            }

            val desiredYaw = if (settings.rotation && (entity is ItemDisplay || !pet.entityTexture.contains(":"))) {
                if (isAnimatedItemDisplay) {
                    tracked.animation?.getFacingYaw(
                        targetLocation.yaw,
                        tick,
                        settings.animation,
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

        val offset = if (includeHorizontalOffset) {
            // Calculate horizontal forward direction from player yaw, ignoring vertical pitch.
            val forward = playerLocation.direction.apply { y = 0.0 }
            if (forward.lengthSquared() > 0.0) {
                forward.normalize()
            }
            val right = forward.clone().rotateAroundY(-Math.PI / 2)
            forward.multiply(-settings.zOffset).add(right.multiply(settings.xOffset))
        } else {
            Vector()
        }

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

        offset.y = smoothedYOffset

        // Use base location, not eyeLocation, for static anchoring
        return playerLocation.add(0.0, DEFAULT_EYE_HEIGHT, 0.0).add(offset)
    }

    private fun addLocalOffset(location: Location, player: Player, sideways: Double, forwardAmount: Double) {
        if (sideways == 0.0 && forwardAmount == 0.0) {
            return
        }

        val forward = player.location.direction.clone().apply { y = 0.0 }
        if (forward.lengthSquared() == 0.0) {
            return
        }

        forward.normalize()
        val right = forward.clone().rotateAroundY(-PI / 2)
        location.add(right.multiply(sideways)).add(forward.multiply(forwardAmount))
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
                if (entity is ItemDisplay) PetAnimation(player.uniqueId) else null
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

    private class PetAnimation(uuid: UUID) {
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

        fun getMotion(
            currentTick: Long,
            animationSettings: AnimationSettings,
            displaySettings: DisplaySettings
        ): PetMotion {
            if (action == PetAction.IDLE) {
                val idleBob = getIdleBob(currentTick, animationSettings, displaySettings)
                if (nextActionAt == Long.MIN_VALUE) {
                    scheduleNextAction(currentTick, animationSettings)
                }
                val crossedNeutral = idleBob == 0.0 || previousIdleBob == 0.0 || idleBob * previousIdleBob < 0.0
                previousIdleBob = idleBob

                if (currentTick >= nextActionAt && crossedNeutral) {
                    startAction(currentTick, animationSettings, displaySettings)
                } else {
                    return idleMotion(idleBob, displaySettings)
                }
            } else if (currentTick - actionStartedAt >= actionDuration) {
                finishAction()
                action = PetAction.IDLE
                idleStartedAt = currentTick
                previousIdleBob = 0.0
                scheduleNextAction(currentTick, animationSettings)
                return idleMotion(0.0, displaySettings)
            }

            val progress = ((currentTick - actionStartedAt).toDouble() / actionDuration).coerceIn(0.0, 1.0)

            return when (action) {
                PetAction.SWAY -> {
                    val envelope = NumberUtils.fastSin(PI * progress)
                    val wave = NumberUtils.fastSin(FULL_CIRCLE * progress) * envelope
                    val base = anchorMotion(displaySettings)
                    PetMotion(
                        sideways = base.sideways + wave * animationSettings.swayDistance * actionStrength,
                        forward = base.forward + (fastCos(FULL_CIRCLE * progress) - 1.0) *
                            envelope * animationSettings.swayForward * actionStrength,
                        yaw = (wave * animationSettings.lookAngle * 0.18 * actionStrength).toFloat(),
                        pitch = (NumberUtils.fastSin(FULL_CIRCLE * 2 * progress) *
                            envelope * animationSettings.swayPitch * actionStrength).toFloat(),
                        roll = (-wave * animationSettings.swayRoll * actionStrength).toFloat(),
                        scaleXZ = base.scaleXZ,
                        scaleY = base.scaleY
                    )
                }

                PetAction.PUFF -> {
                    val puff = when {
                        progress < 0.25 -> smootherStep(progress / 0.25)
                        progress < 0.62 -> 1.0
                        else -> {
                            val settle = (progress - 0.62) / 0.38
                            (1.0 - smootherStep(settle)) +
                                NumberUtils.fastSin(PI * 3 * settle) * (1.0 - settle) * 0.08
                        }
                    }
                    val base = anchorMotion(displaySettings)
                    PetMotion(
                        sideways = base.sideways,
                        forward = base.forward,
                        pitch = (NumberUtils.fastSin(FULL_CIRCLE * 2 * progress) *
                            (1.0 - progress) * 1.5).toFloat(),
                        roll = (NumberUtils.fastSin(FULL_CIRCLE * 3 * progress) *
                            (1.0 - progress) * 2.0).toFloat(),
                        scaleXZ = base.scaleXZ + puff * animationSettings.puffScale * actionStrength,
                        scaleY = base.scaleY + puff * animationSettings.puffScale * 0.85 * actionStrength
                    )
                }

                PetAction.ORBIT -> {
                    val envelopeSin = NumberUtils.fastSin(PI * progress)
                    val envelope = envelopeSin * envelopeSin
                    val angle = lookDirection * FULL_CIRCLE * progress
                    val base = anchorMotion(displaySettings)
                    PetMotion(
                        vertical = NumberUtils.fastSin(angle * 2) *
                            envelope * animationSettings.orbitHeight * 0.35 * actionStrength,
                        sideways = base.sideways + NumberUtils.fastSin(angle) *
                            envelope * animationSettings.orbitRadius * actionStrength,
                        forward = base.forward + (fastCos(angle) - 1.0) *
                            envelope * animationSettings.orbitRadius * 0.55 * actionStrength,
                        yaw = (NumberUtils.fastSin(angle) * envelope *
                            animationSettings.lookAngle * 0.18 * actionStrength).toFloat(),
                        pitch = (NumberUtils.fastSin(angle) * envelope *
                            animationSettings.orbitTilt * 0.25 * actionStrength).toFloat(),
                        roll = (-fastCos(angle) * envelope *
                            animationSettings.orbitTilt * lookDirection * actionStrength).toFloat(),
                        scaleXZ = base.scaleXZ,
                        scaleY = base.scaleY
                    )
                }

                PetAction.LOOK_AROUND -> {
                    val base = anchorMotion(displaySettings)
                    val lookYaw = getCuriousLookYaw(progress, animationSettings)
                    val envelope = NumberUtils.fastSin(PI * progress)
                    PetMotion(
                        sideways = base.sideways,
                        forward = base.forward,
                        yaw = lookYaw.toFloat(),
                        pitch = (NumberUtils.fastSin(FULL_CIRCLE * progress) * envelope * 3.0).toFloat(),
                        roll = (-lookYaw * 0.10).toFloat(),
                        scaleXZ = base.scaleXZ,
                        scaleY = base.scaleY
                    )
                }

                PetAction.BACKFLIP -> {
                    val base = anchorMotion(displaySettings)
                    val arc = NumberUtils.fastSin(PI * progress)
                    val contact = when {
                        progress < 0.16 -> NumberUtils.fastSin(PI * progress / 0.16)
                        progress > 0.84 -> NumberUtils.fastSin(PI * (progress - 0.84) / 0.16)
                        else -> 0.0
                    }
                    PetMotion(
                        vertical = arc * animationSettings.backflipHeight * actionStrength,
                        sideways = base.sideways,
                        forward = base.forward - arc * 0.04 * actionStrength,
                        pitch = (-360.0 * smootherStep(progress) * lookDirection).toFloat(),
                        roll = (NumberUtils.fastSin(FULL_CIRCLE * progress) * 2.0).toFloat(),
                        scaleXZ = 1.0 + contact * animationSettings.flipSquash * 0.5,
                        scaleY = 1.0 - contact * animationSettings.flipSquash + arc * 0.025
                    )
                }

                PetAction.PIROUETTE -> {
                    val base = anchorMotion(displaySettings)
                    val envelope = NumberUtils.fastSin(PI * progress)
                    PetMotion(
                        sideways = base.sideways,
                        forward = base.forward,
                        yaw = (360.0 * smootherStep(progress) * lookDirection).toFloat(),
                        pitch = (envelope * 2.0).toFloat(),
                        roll = (-envelope * lookDirection * 4.0).toFloat(),
                        scaleXZ = 1.0 + envelope * 0.025,
                        scaleY = 1.0 - envelope * 0.035
                    )
                }

                PetAction.BARREL_ROLL -> {
                    val base = anchorMotion(displaySettings)
                    val envelope = NumberUtils.fastSin(PI * progress)
                    PetMotion(
                        vertical = NumberUtils.fastSin(FULL_CIRCLE * progress) * 0.015,
                        sideways = base.sideways,
                        forward = base.forward,
                        yaw = (NumberUtils.fastSin(FULL_CIRCLE * progress) * 4.0).toFloat(),
                        pitch = (envelope * 3.0).toFloat(),
                        roll = (360.0 * smootherStep(progress) * lookDirection).toFloat()
                    )
                }

                PetAction.REPOSITION -> {
                    getRepositionMotion(progress, animationSettings, displaySettings)
                }

                PetAction.IDLE -> idleMotion(0.0, displaySettings)
            }
        }

        fun getFacingYaw(
            playerYaw: Float,
            currentTick: Long,
            settings: AnimationSettings,
            actionYaw: Float
        ): Float {
            if (nextFacingChangeAt == Long.MIN_VALUE || currentTick >= nextFacingChangeAt) {
                idleFacingOffset = (random.nextDouble() * 2.0 - 1.0) * settings.idleLookAngle
                nextFacingChangeAt = currentTick + randomDelay(settings.minLookDelay, settings.maxLookDelay)
            }

            val target = playerYaw + ITEM_DISPLAY_YAW_OFFSET + idleFacingOffset.toFloat()
            if (facingYaw.isNaN()) {
                facingYaw = target
                return facingYaw + actionYaw
            }

            facingYaw += shortestAngle(facingYaw, target) * settings.facingSmoothing.toFloat()
            return facingYaw + actionYaw
        }

        private fun getIdleBob(
            currentTick: Long,
            settings: AnimationSettings,
            displaySettings: DisplaySettings
        ): Double {
            if (!displaySettings.bobbing) {
                return 0.0
            }

            if (idleStartedAt == Long.MIN_VALUE) {
                val randomPhase = random.nextInt(settings.idleBobPeriod.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                idleStartedAt = currentTick - randomPhase
            }

            return NumberUtils.fastSin(
                (currentTick - idleStartedAt).toDouble() / settings.idleBobPeriod * FULL_CIRCLE
            ) * displaySettings.bobbingIntensity
        }

        private fun idleMotion(
            idleBob: Double,
            displaySettings: DisplaySettings
        ): PetMotion = anchorMotion(displaySettings).copy(vertical = idleBob)

        private fun anchorMotion(displaySettings: DisplaySettings): PetMotion = PetMotion(
            sideways = sidePosition(side, displaySettings),
            forward = -displaySettings.zOffset
        )

        private fun getRepositionMotion(
            progress: Double,
            settings: AnimationSettings,
            displaySettings: DisplaySettings
        ): PetMotion {
            val angle = lerp(repositionAngleFrom, repositionAngleTo, smootherStep(progress))
            val desiredSide = kotlin.math.sin(angle) * repositionRadius
            val desiredForward = -kotlin.math.cos(angle) * repositionRadius
            val envelope = NumberUtils.fastSin(PI * progress)

            return PetMotion(
                sideways = desiredSide,
                forward = desiredForward,
                yaw = (-sideFrom * envelope * settings.lookAngle * 0.28 * actionStrength).toFloat(),
                pitch = (NumberUtils.fastSin(FULL_CIRCLE * progress) *
                    envelope * 2.5 * actionStrength).toFloat(),
                roll = (-sideFrom * envelope *
                    settings.sideSwitchBank * actionStrength).toFloat(),
                scaleXZ = 1.0,
                scaleY = 1.0
            )
        }

        private fun getCuriousLookYaw(progress: Double, settings: AnimationSettings): Double {
            val fullLook = lookDirection * settings.lookAngle
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
            animationSettings: AnimationSettings,
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
            val baseDuration = when (action) {
                PetAction.SWAY -> animationSettings.swayDuration
                PetAction.PUFF -> animationSettings.puffDuration
                PetAction.ORBIT -> animationSettings.orbitDuration
                PetAction.LOOK_AROUND -> animationSettings.lookDuration
                PetAction.BACKFLIP -> animationSettings.backflipDuration
                PetAction.PIROUETTE -> animationSettings.pirouetteDuration
                PetAction.BARREL_ROLL -> animationSettings.barrelRollDuration
                PetAction.REPOSITION -> animationSettings.sideSwitchDuration
                PetAction.IDLE -> 1L
            }
            actionDuration = (baseDuration * (0.90 + random.nextDouble() * 0.20))
                .roundToLong()
                .coerceAtLeast(1L)

            lookDirection = if (side > 0.0) -1.0 else 1.0
            if (action == PetAction.REPOSITION) {
                sideFrom = side
                sideTo = -side
                repositionRadius = kotlin.math.hypot(displaySettings.xOffset, displaySettings.zOffset)
                val sideAngle = kotlin.math.atan2(
                    displaySettings.xOffset,
                    abs(displaySettings.zOffset).coerceAtLeast(0.001)
                )
                repositionAngleFrom = sideFrom * sideAngle
                repositionAngleTo = sideTo * sideAngle
            }
        }

        private fun finishAction() {
            if (action == PetAction.REPOSITION) {
                side = sideTo
            }
        }

        private fun scheduleNextAction(currentTick: Long, settings: AnimationSettings) {
            val chance = settings.chancePerSecond
            if (chance <= 0.0) {
                nextActionAt = Long.MAX_VALUE
                return
            }

            val secondsUntilAction = if (chance >= 1.0) {
                1L
            } else {
                val failedSeconds = (
                    ln1p(-random.nextDouble()) / ln1p(-chance)
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

        private fun sidePosition(side: Double, settings: DisplaySettings): Double =
            side * settings.xOffset
    }

    private enum class PetAction {
        IDLE,
        SWAY,
        PUFF,
        ORBIT,
        LOOK_AROUND,
        BACKFLIP,
        PIROUETTE,
        BARREL_ROLL,
        REPOSITION
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
        val animation: AnimationSettings = AnimationSettings()
    )

    private data class AnimationSettings(
        val enabled: Boolean = false,
        val chancePerSecond: Double = 0.25,
        val idleBobPeriod: Long = 64L,
        val swayDuration: Long = 40L,
        val swayDistance: Double = 0.14,
        val swayForward: Double = 0.05,
        val swayRoll: Double = 9.0,
        val swayPitch: Double = 3.0,
        val puffDuration: Long = 32L,
        val puffScale: Double = 0.10,
        val orbitDuration: Long = 44L,
        val orbitRadius: Double = 0.16,
        val orbitHeight: Double = 0.035,
        val orbitTilt: Double = 8.0,
        val lookDuration: Long = 44L,
        val lookAngle: Double = 25.0,
        val backflipDuration: Long = 25L,
        val backflipHeight: Double = 0.10,
        val flipSquash: Double = 0.10,
        val pirouetteDuration: Long = 22L,
        val barrelRollDuration: Long = 23L,
        val sideSwitchDuration: Long = 44L,
        val sideSwitchBank: Double = 9.0,
        val facingSmoothing: Double = 0.12,
        val idleLookAngle: Double = 32.0,
        val minLookDelay: Long = 60L,
        val maxLookDelay: Long = 140L
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
