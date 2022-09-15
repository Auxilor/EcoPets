package com.willfp.ecopets.pets

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.placeholder.PlayerStaticPlaceholder
import com.willfp.eco.core.placeholder.PlayerlessPlaceholder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toNiceString
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.api.event.PlayerPetExpGainEvent
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.pets.entity.PetEntity
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.conditions.ConfiguredCondition
import com.willfp.libreforge.effects.ConfiguredEffect
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class Pet(
    val id: String,
    val config: Config,
    private val plugin: EcoPetsPlugin
) {
    val name = config.getFormattedString("name")
    val description = config.getFormattedString("description")

    val levelKey: PersistentDataKey<Int> = PersistentDataKey(
        EcoPetsPlugin.instance.namespacedKeyFactory.create("${id}_level"),
        PersistentDataKeyType.INT,
        0
    )

    val xpKey: PersistentDataKey<Double> = PersistentDataKey(
        EcoPetsPlugin.instance.namespacedKeyFactory.create("${id}_xp"),
        PersistentDataKeyType.DOUBLE,
        0.0
    )

    private val spawnEggBacker: ItemStack? = run {
        val enabled = config.getBool("spawn-egg.enabled")
        if (!enabled) {
            return@run null
        }

        val lookup = Items.lookup(config.getString("spawn-egg.item"))

        if (lookup is EmptyTestableItem) {
            return@run null
        }

        val name = config.getFormattedStringOrNull("spawn-egg.name")

        val item = ItemStackBuilder(lookup)
            .addLoreLines(config.getFormattedStrings("spawn-egg.lore"))
            .apply {
                if (name != null) {
                    setDisplayName(name)
                }
            }
            .build().apply { petEgg = this@Pet }

        val key = plugin.namespacedKeyFactory.create("${this.id}_spawn_egg")

        Items.registerCustomItem(
            key,
            CustomItem(
                key,
                { it.petEgg == this },
                item
            )
        )

        item
    }

    private fun inventoryItemBacker(player: Player, clickedPet: Pet): ItemStack? = run {
        val enabled = config.getBool("inventory-item.enabled")
        if (!enabled) {
            player.sendMessage(plugin.langYml.getMessage("cannot-take-out"))
            return@run null
        }

        val lookup = Items.lookup(config.getString("inventory-item.item"))

        if (lookup is EmptyTestableItem) {
            return@run null
        }

        val item = ItemStackBuilder(lookup)
            .addLoreLines(
                injectPlaceholdersInto(config.getStrings("inventory-item.lore"), player))
            .setDisplayName(
                config.getString("inventory-item.name")
                    .replace("%level%", player.getPetLevel(clickedPet).toString())
            )

            .build().apply { petEgg = this@Pet }

        item.fast().persistentDataContainer.set(SpawnEggHandler(plugin).level, PersistentDataType.INTEGER, player.getPetLevel(clickedPet))
        item.fast().persistentDataContainer.set(SpawnEggHandler(plugin).xp, PersistentDataType.DOUBLE, player.getPetXP(clickedPet))

        val key = plugin.namespacedKeyFactory.create("${this.id}_spawn_egg")

        Items.registerCustomItem(
            key,
            CustomItem(
                key,
                { it.petEgg == this },
                item
            )
        )

        item
    }

    val spawnEgg: ItemStack?
        get() = this.spawnEggBacker?.clone()

    fun inventoryItem(player: Player, pet: Pet): ItemStack? {
        return this.inventoryItemBacker(player, pet)?.clone()
    }


    val recipe = run {
        val egg = spawnEgg

        if (egg == null || !config.getBool("spawn-egg.craftable")) {
            null
        } else {
            Recipes.createAndRegisterRecipe(
                plugin,
                "${this.id}_spawn_egg",
                egg,
                config.getStrings("spawn-egg.recipe"),
                config.getStringOrNull("spawn-egg.recipe-permission")
            )
        }
    }

    val entityTexture = config.getString("entity-texture")

    private val levelXpRequirements = listOf(0) + config.getInts("level-xp-requirements")

    val maxLevel = levelXpRequirements.size

    val levelGUI = PetLevelGUI(plugin, this)

    private val baseItem: ItemStack = Items.lookup(config.getString("icon")).item

    private val effects: Set<ConfiguredEffect>
    private val conditions: Set<ConfiguredCondition>

    private val levels = Caffeine.newBuilder()
        .build<Int, PetLevel>()
    private val effectsDescription = Caffeine.newBuilder()
        .build<Int, List<String>>()
    private val rewardsDescription = Caffeine.newBuilder()
        .build<Int, List<String>>()
    private val levelUpMessages = Caffeine.newBuilder()
        .build<Int, List<String>>()

    private val levelCommands = mutableMapOf<Int, MutableList<String>>()

    private val levelPlaceholders = config.getSubsections("level-placeholders")
        .map { sub ->
            LevelPlaceholder(
                sub.getString("id")
            ) {
                NumberUtils.evaluateExpression(
                    sub.getString("value")
                        .replace("%level%", it.toString())
                ).toNiceString()
            }
        }

    private val petXpGains = config.getSubsections("xp-gain-methods").mapNotNull {
        val trigger = Triggers.getById(it.getString("id")) ?: return@mapNotNull null
        val multiplier = it.getDouble("multiplier")
        val conditions = it.getSubsections("conditions")
            .mapNotNull { cfg -> Conditions.compile(cfg, "Pet $id XP Gain methods") }

        trigger to PetXPGain(
            trigger,
            multiplier,
            conditions,
            it.getSubsection("filters")
        )
    }.toMap()

    init {
        config.injectPlaceholders(
            PlayerStaticPlaceholder(
                "level"
            ) { p ->
                p.getPetLevel(this).toString()
            }
        )

        effects = config.getSubsections("effects").mapNotNull {
            Effects.compile(it, "Pet $id")
        }.toSet()

        conditions = config.getSubsections("conditions").mapNotNull {
            Conditions.compile(it, "Pet $id")
        }.toSet()

        for (string in config.getStrings("level-commands")) {
            val split = string.split(":")

            if (split.size == 1) {
                for (level in 1..maxLevel) {
                    val commands = levelCommands[level] ?: mutableListOf()
                    commands.add(string)
                    levelCommands[level] = commands
                }
            } else {
                val level = split[0].toInt()

                val command = string.removePrefix("$level:")
                val commands = levelCommands[level] ?: mutableListOf()
                commands.add(command)
                levelCommands[level] = commands
            }
        }

        PlayerPlaceholder(
            plugin,
            "${id}_percentage_progress"
        ) {
            (it.getPetProgress(this) * 100).toNiceString()
        }.register()

        PlayerPlaceholder(
            plugin,
            id
        ) {
            it.getPetLevel(this).toString()
        }.register()

        PlayerPlaceholder(
            plugin,
            "${id}_current_xp"
        ) {
            NumberUtils.format(it.getPetXP(this))
        }.register()

        PlayerPlaceholder(
            plugin,
            "${id}_required_xp"
        ) {
            it.getPetXPRequired(this).toString()
        }.register()

        PlayerlessPlaceholder(
            plugin,
            "${id}_name"
        ) {
            this.name
        }.register()

        PlayerPlaceholder(
            plugin,
            "${id}_level"
        ) {
            it.getPetLevel(this).toString()
        }.register()
    }

    fun makePetEntity(): PetEntity {
        return PetEntity.create(this)
    }

    fun getLevel(level: Int): PetLevel = levels.get(level) {
        PetLevel(this, it, effects, conditions)
    }

    private fun getLevelUpMessages(level: Int, whitespace: Int = 0): List<String> = levelUpMessages.get(level) {
        var highestConfiguredLevel = 1
        for (messagesLevel in this.config.getSubsection("level-up-messages").getKeys(false).map { it.toInt() }) {
            if (messagesLevel > level) {
                continue
            }

            if (messagesLevel > highestConfiguredLevel) {
                highestConfiguredLevel = messagesLevel
            }
        }

        this.config.getStrings("level-up-messages.$highestConfiguredLevel")
            .map {
                levelPlaceholders.format(it, level)
            }
            .map {
                " ".repeat(whitespace) + it
            }
    }

    private fun getEffectsDescription(level: Int, whitespace: Int = 0): List<String> = effectsDescription.get(level) {
        var highestConfiguredLevel = 1
        for (messagesLevel in this.config.getSubsection("effects-description").getKeys(false).map { it.toInt() }) {
            if (messagesLevel > level) {
                continue
            }

            if (messagesLevel > highestConfiguredLevel) {
                highestConfiguredLevel = messagesLevel
            }
        }

        this.config.getStrings("effects-description.$highestConfiguredLevel")
            .map {
                levelPlaceholders.format(it, level)
            }
            .map {
                " ".repeat(whitespace) + it
            }
    }

    private fun getRewardsDescription(level: Int, whitespace: Int = 0): List<String> = rewardsDescription.get(level) {
        var highestConfiguredLevel = 1
        for (messagesLevel in this.config.getSubsection("rewards-description").getKeys(false).map { it.toInt() }) {
            if (messagesLevel > level) {
                continue
            }

            if (messagesLevel > highestConfiguredLevel) {
                highestConfiguredLevel = messagesLevel
            }
        }

        this.config.getStrings("rewards-description.$highestConfiguredLevel")
            .map {
                levelPlaceholders.format(it, level)
            }
            .map {
                " ".repeat(whitespace) + it
            }
    }

    fun injectPlaceholdersInto(lore: List<String>, player: Player, forceLevel: Int? = null): List<String> {
        val withPlaceholders = lore
            .map {
                it.replace("%percentage_progress%", (player.getPetProgress(this) * 100).toNiceString())
                    .replace("%current_xp%", player.getPetXP(this).toNiceString())
                    .replace("%required_xp%", this.getExpForLevel(player.getPetLevel(this) + 1).let { req ->
                        if (req == Int.MAX_VALUE) {
                            plugin.langYml.getFormattedString("infinity")
                        } else {
                            req.toNiceString()
                        }
                    }
                    )
                    .replace("%description%", this.description)
                    .replace("%pet%", this.name)
                    .replace("%level%", (forceLevel ?: player.getPetLevel(this)).toString())
            }
            .toMutableList()

        val processed = mutableListOf<List<String>>()

        for (s in withPlaceholders) {
            val whitespace = s.length - s.replace(" ", "").length

            processed.add(
                if (s.contains("%effects%")) {
                    getEffectsDescription(forceLevel ?: player.getPetLevel(this), whitespace)
                } else if (s.contains("%rewards%")) {
                    getRewardsDescription(forceLevel ?: player.getPetLevel(this), whitespace)
                } else if (s.contains("%level_up_messages%")) {
                    getLevelUpMessages(forceLevel ?: player.getPetLevel(this), whitespace)
                } else {
                    listOf(s)
                }
            )
        }

        return processed.flatten().formatEco(player)
    }

    fun getIcon(player: Player): ItemStack {
        val base = baseItem.clone()

        val level = player.getPetLevel(this)
        val isActive = player.activePet == this

        return ItemStackBuilder(base)
            .setDisplayName(
                plugin.configYml.getFormattedString("gui.pet-icon.name")
                    .replace("%level%", level.toString())
                    .replace("%pet%", this.name)
            )
            .addLoreLines {
                injectPlaceholdersInto(plugin.configYml.getStrings("gui.pet-icon.lore"), player) +
                        if (isActive) plugin.configYml.getStrings("gui.pet-icon.active-lore") else
                            plugin.configYml.getStrings("gui.pet-icon.not-active-lore")
            }
            .build()
    }

    fun getPetInfoIcon(player: Player): ItemStack {
        val base = baseItem.clone()
        return ItemStackBuilder(base)
            .setDisplayName(
                plugin.configYml.getFormattedString("gui.pet-info.active.name")
                    .replace("%level%", player.getPetLevel(this).toString())
                    .replace("%pet%", this.name)
            )
            .addLoreLines {
                injectPlaceholdersInto(plugin.configYml.getStrings("gui.pet-info.active.lore"), player)
            }
            .build()
    }

    fun getExpForLevel(level: Int): Int {
        if (level < 1 || level > maxLevel) {
            return Int.MAX_VALUE
        }

        return levelXpRequirements[level - 1]
    }

    fun executeLevelCommands(player: Player, level: Int) {
        val commands = levelCommands[level] ?: emptyList()

        for (command in commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.name))
        }
    }

    fun getPetXPGain(trigger: Trigger): PetXPGain? {
        return petXpGains[trigger]
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Pet) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }
}

private class LevelPlaceholder(
    val id: String,
    private val function: (Int) -> String
) {
    operator fun invoke(level: Int) = function(level)
}

private fun Collection<LevelPlaceholder>.format(string: String, level: Int): String {
    var process = string
    for (placeholder in this) {
        process = process.replace("%${placeholder.id}%", placeholder(level))
    }
    return process
}

private val activePetKey: PersistentDataKey<String> = PersistentDataKey(
    EcoPetsPlugin.instance.namespacedKeyFactory.create("active_pet"),
    PersistentDataKeyType.STRING,
    ""
)

private val petEggKey = EcoPetsPlugin.instance.namespacedKeyFactory.create("pet_egg")

var ItemStack.petEgg: Pet?
    get() = Pets.getByID(this.fast().persistentDataContainer.get(petEggKey, PersistentDataType.STRING) ?: "")
    set(value) {
        value ?: return
        this.fast().persistentDataContainer.set(petEggKey, PersistentDataType.STRING, value.id)
    }

var OfflinePlayer.activePet: Pet?
    get() = Pets.getByID(this.profile.read(activePetKey))
    set(value) = this.profile.write(activePetKey, value?.id ?: "")

val OfflinePlayer.activePetLevel: PetLevel?
    get() {
        val active = this.activePet ?: return null
        return this.getPetLevelObject(active)
    }

fun OfflinePlayer.getPetLevel(pet: Pet): Int =
    this.profile.read(pet.levelKey)

fun OfflinePlayer.setPetLevel(pet: Pet, level: Int) =
    this.profile.write(pet.levelKey, level)

fun OfflinePlayer.getPetProgress(pet: Pet): Double {
    val currentXP = this.getPetXP(pet)
    val requiredXP = pet.getExpForLevel(this.getPetLevel(pet) + 1)
    return currentXP / requiredXP
}

fun OfflinePlayer.getPetLevelObject(pet: Pet): PetLevel =
    pet.getLevel(this.getPetLevel(pet))

fun OfflinePlayer.hasPet(pet: Pet): Boolean =
    this.getPetLevel(pet) > 0

fun OfflinePlayer.getPetXP(pet: Pet): Double =
    this.profile.read(pet.xpKey)

fun OfflinePlayer.setPetXP(pet: Pet, xp: Double) =
    this.profile.write(pet.xpKey, xp)

fun OfflinePlayer.getPetXPRequired(pet: Pet): Int =
    pet.getExpForLevel(this.getPetLevel(pet) + 1)

private val expMultiplierCache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.SECONDS)
    .build<Player, Double> {
        it.cacheSkillExperienceMultiplier()
    }

val Player.petExperienceMultiplier: Double
    get() = expMultiplierCache.get(this)

private fun Player.cacheSkillExperienceMultiplier(): Double {
    if (this.hasPermission("ecopets.xpmultiplier.quadruple")) {
        return 4.0
    }

    if (this.hasPermission("ecopets.xpmultiplier.triple")) {
        return 3.0
    }

    if (this.hasPermission("ecopets.xpmultiplier.double")) {
        return 2.0
    }

    if (this.hasPermission("ecopets.xpmultiplier.50percent")) {
        return 1.5
    }

    val prefix = "ecopets.xpmultiplier."
    for (permissionAttachmentInfo in this.effectivePermissions) {
        val permission = permissionAttachmentInfo.permission
        if (permission.startsWith(prefix)) {
            return ((permission.substring(permission.lastIndexOf(".") + 1).toDoubleOrNull() ?: 100.0) / 100) + 1
        }
    }

    return 1.0
}

fun Player.givePetExperience(pet: Pet, experience: Double, noMultiply: Boolean = false) {
    val exp = abs(if (noMultiply) experience else experience * this.petExperienceMultiplier)

    val gainEvent = PlayerPetExpGainEvent(this, pet, exp, !noMultiply)
    Bukkit.getPluginManager().callEvent(gainEvent)

    if (gainEvent.isCancelled) {
        return
    }

    this.giveExactPetExperience(pet, gainEvent.amount)
}

fun Player.giveExactPetExperience(pet: Pet, experience: Double) {
    val level = this.getPetLevel(pet)

    val progress = this.getPetXP(pet) + experience

    if (progress >= pet.getExpForLevel(level + 1) && level + 1 <= pet.maxLevel) {
        val overshoot = progress - pet.getExpForLevel(level + 1)
        this.setPetXP(pet, 0.0)
        this.setPetLevel(pet, level + 1)
        val levelUpEvent = PlayerPetLevelUpEvent(this, pet, level + 1)
        Bukkit.getPluginManager().callEvent(levelUpEvent)
        this.giveExactPetExperience(pet, overshoot)
    } else {
        this.setPetXP(pet, progress)
    }
}
