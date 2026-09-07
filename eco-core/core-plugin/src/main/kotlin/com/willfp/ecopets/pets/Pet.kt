package com.willfp.ecopets.pets

import com.willfp.eco.core.cache.EcoCache
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.eco.core.price.Prices
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.placeholder.PlayerStaticPlaceholder
import com.willfp.eco.core.placeholder.PlayerlessPlaceholder
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.core.progression.LevelCurve
import com.willfp.eco.core.progression.LevelCurves
import com.willfp.eco.core.progression.LevelProgression
import com.willfp.eco.core.progression.StopReason
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.recipe.recipes.CraftingRecipe
import com.willfp.eco.core.registry.Registrable
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.NumberUtils.evaluateExpression
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toNiceString
import com.willfp.eco.util.toNumeral
import com.willfp.ecopets.api.event.PlayerPetActivateEvent
import com.willfp.ecopets.api.event.PlayerPetDeactivateEvent
import com.willfp.ecopets.api.event.PlayerPetExpGainEvent
import com.willfp.ecopets.api.event.PlayerPetLevelUpEvent
import com.willfp.ecopets.pets.entity.PetEntity
import com.willfp.ecopets.plugin
import com.willfp.ecopets.price.PriceFactoryPetLevel
import com.willfp.ecopets.util.LevelInjectable
import com.willfp.libreforge.SimpleProvidedHolder
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.counters.Counters
import com.willfp.libreforge.effects.EffectList
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.executors.impl.NormalExecutorFactory
import com.willfp.libreforge.toDispatcher
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class Pet(
    val id: String,
    val config: Config
) : Registrable {

    val name = config.getFormattedString("name")

    val description = config.getFormattedString("description")

    val levelKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_level"),
        PersistentDataKeyType.INT,
        0
    )

    val xpKey: PersistentDataKey<Double> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_xp"), PersistentDataKeyType.DOUBLE, 0.0
    )

    private val levelPlaceholders = config.getSubsections("level-placeholders")
        .map { sub ->
            LevelPlaceholder(
                sub.getString("id")
            ) {
                evaluateExpression(
                    sub.getString("value")
                        .replace("%level%", it.toString())
                )
            }
        }

    // Resolved and cloned at init time (before custom item registration) so that
    // subsequent makeSpawnEgg calls always start from the clean raw item and never
    // accidentally pick up the registered custom item's already-baked lore.
    private val eggBaseItem: ItemStack? = run {
        if (!config.getBool("spawn-egg.enabled")) return@run null
        val lookup = Items.lookup(config.getString("spawn-egg.item"))
        if (lookup is EmptyTestableItem) null else lookup.item.clone()
    }

    private val eggBaseName = config.getFormattedStringOrNull("spawn-egg.name")
    private val eggBaseLore = config.getFormattedStrings("spawn-egg.lore")

    private fun formatEggText(text: String, level: Int, xp: Double): String {
        var result = text
            .replace("%pet%", this.name)
            .replace("%description%", this.description)
            .replace("%current_xp%", xp.toNiceString())
            .replace("%level%", level.toString())
            .replace("%level_numeral%", level.toNumeral())
        result = EGG_LEVEL_REGEX.replace(result) { match ->
            val offset = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            val isNumeral = match.groupValues[2].isNotEmpty()
            val newLevel = level + offset
            if (isNumeral) newLevel.toNumeral() else newLevel.toString()
        }
        return levelPlaceholders.format(result, level)
    }

    fun makeSpawnEgg(level: Int = 1, xp: Double = 0.0): ItemStack? {
        val base = eggBaseItem?.clone() ?: return null
        val item = ItemStackBuilder(base)
            .addLoreLines(eggBaseLore.map { formatEggText(it, level, xp) })
            .apply {
                if (eggBaseName != null) {
                    setDisplayName(formatEggText(eggBaseName, level, xp))
                }
            }
            .build()
        item.petEgg = this
        item.petEggLevel = level
        item.petEggXp = xp
        return item
    }

    val spawnEgg: ItemStack?
        get() = makeSpawnEgg(1, 0.0)

    val recipe: CraftingRecipe? = spawnEgg
        ?.takeIf { config.getBool("spawn-egg.craftable") }
        ?.let { egg ->
            val recipeStrings = config.getStrings("spawn-egg.recipe")
            if (recipeStrings.isEmpty()) return@let null

            Recipes.createAndRegisterRecipe(
                plugin,
                "${id}_spawn_egg",
                egg,
                recipeStrings,
                config.getStringOrNull("spawn-egg.recipe-permission"),
                config.getBool("spawn-egg.shapeless")
            )
        }

    val entityTexture = config.getString("entity-texture")

    val trail = PetTrail.fromConfig(config.getSubsection("trail"))

    private val parsedCurve = LevelCurves.parse(
        config.getStringOrNull("xp-formula"),
        config.getDoublesOrNull("level-xp-requirements"),
        config.getIntOrNull("max-level"),
        startLevel = 0,
        // The `listOf(0) + ...` padding this replaces is preserved, not removed. Adopting a
        // pet sets its level to 0, and hasPet is `level > 0`, so the free 0 -> 1 transition is
        // how a pet becomes owned. Dropping it would break adoption. The auto-adopt defect it
        // enabled is fixed by the ownership guard in the give/giveExact functions instead.
        freeFirstLevel = true
    ) { expression, level ->
        evaluateExpression(expression, placeholderContext(injectable = LevelInjectable(level - 1)))
    }

    val curve: LevelCurve = parsedCurve.curve

    val maxLevel: Int
        get() = curve.maxLevel

    private val warnedBrokenCurveLevels = mutableSetOf<Int>()

    /**
     * Log a broken-curve warning once per level, rather than on every XP gain that hits it.
     */
    fun warnBrokenCurveOnce(level: Int) {
        if (warnedBrokenCurveLevels.add(level)) {
            plugin.logger.warning("Pet $id: xp requirement for level $level is not usable - progression stopped there")
        }
    }

    val withdrawable = config.getBool("spawn-egg.withdrawable")

    val withdrawPrice: ConfiguredPrice by lazy {
        ConfiguredPrice.create(config.getSubsection("spawn-egg.withdraw-price"))
            ?: ConfiguredPrice.FREE
    }

    val levelGUI = PetLevelGUI(this)

    private val baseItem: ItemStack = Items.lookup(config.getString("icon")).item

    private val effects: EffectList

    private val conditions: ConditionList

    private val activateConditions: ConditionList

    private val levels = EcoCache.builder<Int, PetLevel>().build()

    private val effectsDescription = EcoCache.builder<Int, List<String>>().build()

    private val rewardsDescription = EcoCache.builder<Int, List<String>>().build()

    private val levelUpMessages = EcoCache.builder<Int, List<String>>().build()

    internal val priceFactory = PriceFactoryPetLevel(this)

    private val petXpGains = config.getSubsections("xp-gain-methods").mapNotNull {
        Counters.compile(it, ViolationContext(plugin, "Pet $id"))
    }

    init {
        // Logged once at load, not per XP gain: a broken curve recurs on every grant.
        for (problem in parsedCurve.problems) {
            plugin.logger.warning("Pet $id: ${problem.path} - ${problem.message}")
        }

        config.injectPlaceholders(
            PlayerStaticPlaceholder("percentage_progress") {
                (it.getPetProgress(this) * 100).toNiceString()
            },
            PlayerStaticPlaceholder("current_xp") {
                it.getPetXP(this).toNiceString()
            },
            PlayerStaticPlaceholder("required_xp") {
                this.getFormattedExpForLevel(it.getPetLevel(this) + 1)
            },
            PlayerStaticPlaceholder("description") {
                this.description
            },
            PlayerStaticPlaceholder("pet") {
                this.name
            },
            PlayerStaticPlaceholder("level") {
                it.getPetLevel(this).toString()
            },
            PlayerStaticPlaceholder("level_numeral") {
                it.getPetLevel(this).toNumeral()
            },
            PlayerStaticPlaceholder("withdraw_price") {
                this.withdrawPrice.getDisplay(it)
            }
        )

        effects = Effects.compile(
            config.getSubsections("effects"),
            ViolationContext(plugin, "Pet $id")
        )

        conditions = Conditions.compile(
            config.getSubsections("conditions"),
            ViolationContext(plugin, "Pet $id")
        )

        activateConditions = Conditions.compile(
            config.getSubsections("activate-conditions"),
            ViolationContext(plugin, "Pet $id activate-conditions")
        )

        PlayerPlaceholder(
            plugin,
            "active_pet_level"
        ) {
            it.activePet?.let { pet -> it.getPetLevel(pet).toString() } ?: ""
        }.register()

        PlayerPlaceholder(
            plugin,
            "active_pet_level_numeral"
        ) {
            it.activePet?.let { pet -> it.getPetLevel(pet).toNumeral() } ?: ""
        }.register()

        PlayerPlaceholder(
            plugin,
            "active_pet_current_xp"
        ) {
            it.activePet?.let { pet -> it.getPetXP(pet).toNiceString() } ?: ""
        }.register()

        PlayerPlaceholder(
            plugin,
            "active_pet_required_xp"
        ) {
            it.activePet?.let { pet -> pet.getFormattedExpForLevel(it.getPetLevel(pet) + 1) } ?: ""
        }.register()

        PlayerPlaceholder(
            plugin,
            "active_pet_percentage_progress"
        ) {
            it.activePet?.let { pet -> (it.getPetProgress(pet) * 100).toNiceString() } ?: ""
        }.register()

        PlayerPlaceholder(
            plugin,
            "active_pet_description"
        ) {
            it.activePet?.description ?: ""
        }.register()

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

        PlayerPlaceholder(
            plugin,
            "${id}_can_activate"
        ) {
            canActivate(it).toString()
        }.register()

        PlayerPlaceholder(
            plugin,
            "${id}_withdraw_price"
        ) {
            this.withdrawPrice.getDisplay(it)
        }.register()

        makeSpawnEgg(1, 0.0)?.let { representative ->
            val key = plugin.namespacedKeyFactory.create("${this.id}_spawn_egg")
            Items.registerCustomItem(
                key,
                CustomItem(key, { it.petEgg == this }, representative)
            )
        }
    }

    val levelUpEffects = Effects.compileChain(
        config.getSubsections("level-up-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Job $id level-up-effects")
    )

    fun makePetEntity(): PetEntity {
        return PetEntity.create(this)
    }

    fun getLevel(level: Int): PetLevel = levels.get(level) {
        PetLevel(this, it, effects, conditions)
    }

    fun canActivate(player: Player): Boolean {
        if (activateConditions.isEmpty()) return true
        val petLevel = getLevel(player.getPetLevel(this))
        return activateConditions.areMet(player.toDispatcher(), SimpleProvidedHolder(petLevel))
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
        val level = forceLevel ?: player.getPetLevel(this)
        val regex = Regex("%level_(-?\\d+)(_numeral)?%")

        val withPlaceholders = lore.map { line ->
            var result = line
                .replace("%percentage_progress%", (player.getPetProgress(this) * 100).toNiceString())
                .replace("%current_xp%", player.getPetXP(this).toNiceString())
                .replace("%required_xp%", this.getFormattedExpForLevel(level + 1))
                .replace("%description%", this.description)
                .replace("%pet%", this.name)
                .replace("%level%", level.toString())
                .replace("%level_numeral%", level.toNumeral())

            // Handle dynamic %level_X% and %level_X_numeral%
            result = regex.replace(result) { match ->
                val offset = match.groupValues[1].toIntOrNull() ?: return@replace match.value
                val isNumeral = match.groupValues[2].isNotEmpty()
                val newLevel = level + offset

                if (isNumeral) newLevel.toNumeral() else newLevel.toString()
            }

            result
        }.toMutableList()

        val processed = mutableListOf<List<String>>()

        for (s in withPlaceholders) {
            val whitespace = s.length - s.replace(" ", "").length

            processed.add(
                when {
                    s.contains("%effects%") -> getEffectsDescription(level, whitespace)
                    s.contains("%rewards%") -> getRewardsDescription(level, whitespace)
                    s.contains("%level_up_messages%") -> getLevelUpMessages(level, whitespace)
                    else -> listOf(s)
                }
            )
        }

        return processed.flatten().formatEco(player)
    }


    override fun onRegister() {
        petXpGains.forEach { it.bind(PetXPAccumulator(this)) }
        Prices.registerPriceFactory(priceFactory)
    }

    override fun onRemove() {
        petXpGains.forEach { it.unbind() }
        Prices.unregisterPriceFactory(priceFactory)
    }

    fun getIcon(player: Player): ItemStack {
        val base = baseItem.clone()

        val level = player.getPetLevel(this)
        val isActive = player.activePet == this

        val baseLoreLocation = if (level == this.maxLevel) "max-level-lore" else "lore"

        return ItemStackBuilder(base)
            .setDisplayName(
                plugin.configYml.getFormattedString("gui.pet-icon.name")
                    .replace("%level%", level.toString())
                    .replace("%pet%", this.name)
            )
            .addLoreLines {
                injectPlaceholdersInto(plugin.configYml.getStrings("gui.pet-icon.$baseLoreLocation"), player) +
                        if (isActive) plugin.configYml.getStrings("gui.pet-icon.active-lore") else
                            plugin.configYml.getStrings("gui.pet-icon.not-active-lore")
            }
            .build()
    }

    fun getPetInfoIcon(player: Player): ItemStack {
        val base = baseItem.clone()

        val prefix = if (player.getPetLevel(this) == this.maxLevel) "max-level-" else ""

        return ItemStackBuilder(base)
            .setDisplayName(
                plugin.configYml.getFormattedString("gui.pet-info.active.name")
                    .replace("%level%", player.getPetLevel(this).toString())
                    .replace("%pet%", this.name)
            )
            .addLoreLines {
                injectPlaceholdersInto(plugin.configYml.getStrings("gui.pet-info.active.${prefix}lore"), player)
            }
            .build()
    }

    /**
     * Get the XP required to reach [level].
     *
     * Semantic change from the previous implementation: [level] == 1 now returns
     * POSITIVE_INFINITY rather than 0.0, because the free 0 -> 1 transition is handled by
     * [LevelCurve.freeLevel] via LevelProgression.progress rather than being priced as zero.
     * Every other level is unchanged.
     */
    fun getExpForLevel(level: Int): Double = curve.xpToReach(level)

    fun getFormattedExpForLevel(level: Int): String {
        val required = getExpForLevel(level)
        return if (required.isInfinite()) {
            plugin.langYml.getFormattedString("infinity")
        } else {
            required.toNiceString()
        }
    }


    override fun getID(): String {
        return this.id
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
    private val function: (Int) -> Double
) {
    operator fun invoke(level: Int) = function(level)
}

private fun Collection<LevelPlaceholder>.format(string: String, level: Int): String {
    var process = string
    for (placeholder in this) {
        process = process.replace("%${placeholder.id}%", placeholder(level).toNiceString())
    }
    return process
}

private val EGG_LEVEL_REGEX = Regex("%level_([+-]?\\d+)(_numeral)?%")

private val activePetKey: PersistentDataKey<String> = PersistentDataKey(
    plugin.namespacedKeyFactory.create("active_pet"),
    PersistentDataKeyType.STRING,
    ""
)

private val shouldHidePetKey: PersistentDataKey<Boolean> = PersistentDataKey(
    plugin.namespacedKeyFactory.create("hide_pet"),
    PersistentDataKeyType.BOOLEAN,
    false
)

internal val petEggKey = plugin.namespacedKeyFactory.create("pet_egg")
internal val petEggLevelKey = plugin.namespacedKeyFactory.create("pet_egg_level")
internal val petEggXpKey = plugin.namespacedKeyFactory.create("pet_egg_xp")

var ItemStack.petEgg: Pet?
    get() = Pets.getByID(this.fast().persistentDataContainer.get(petEggKey, PersistentDataType.STRING) ?: "")
    set(value) {
        value ?: return
        this.fast().persistentDataContainer.set(petEggKey, PersistentDataType.STRING, value.id)
    }

var ItemStack.petEggLevel: Int
    get() = this.fast().persistentDataContainer.get(petEggLevelKey, PersistentDataType.INTEGER) ?: 1
    set(value) {
        this.fast().persistentDataContainer.set(petEggLevelKey, PersistentDataType.INTEGER, value)
    }

var ItemStack.petEggXp: Double
    get() = this.fast().persistentDataContainer.get(petEggXpKey, PersistentDataType.DOUBLE) ?: 0.0
    set(value) {
        this.fast().persistentDataContainer.set(petEggXpKey, PersistentDataType.DOUBLE, value)
    }

var OfflinePlayer.activePet: Pet?
    get() = Pets.getByID(this.profile.read(activePetKey))
    set(value) {
        if (value == null) {
            val deactivateEvent = PlayerPetDeactivateEvent(this)
            Bukkit.getPluginManager().callEvent(deactivateEvent)
            if (deactivateEvent.isCancelled) return
        } else {
            val activateEvent = PlayerPetActivateEvent(this, value)
            Bukkit.getPluginManager().callEvent(activateEvent)
            if (activateEvent.isCancelled) return
        }
        this.profile.write(activePetKey, value?.id ?: "")
    }

val OfflinePlayer.activePetLevel: PetLevel?
    get() {
        val active = this.activePet ?: return null
        return this.getPetLevelObject(active)
    }

var OfflinePlayer.shouldHidePet: Boolean
    get() = this.profile.read(shouldHidePetKey)
    set(value) = this.profile.write(shouldHidePetKey, value)

fun OfflinePlayer.getPetLevel(pet: Pet): Int =
    this.profile.read(pet.levelKey)

fun OfflinePlayer.setPetLevel(pet: Pet, level: Int) =
    this.profile.write(pet.levelKey, level)

fun OfflinePlayer.getPetProgress(pet: Pet): Double {
    val level = this.getPetLevel(pet)

    return LevelProgression.progressFraction(
        this.getPetXP(pet),
        pet.curve.xpToReach(level + 1),
        level >= pet.maxLevel
    )
}

fun OfflinePlayer.getPetLevelObject(pet: Pet): PetLevel =
    pet.getLevel(this.getPetLevel(pet))

fun OfflinePlayer.hasPet(pet: Pet): Boolean =
    this.getPetLevel(pet) > 0

fun OfflinePlayer.getPetXP(pet: Pet): Double =
    this.profile.read(pet.xpKey)

fun OfflinePlayer.setPetXP(pet: Pet, xp: Double) =
    this.profile.write(pet.xpKey, xp)

fun OfflinePlayer.getPetXPRequired(pet: Pet) =
    this.profile.read(pet.xpKey)

/**
 * Reject an XP amount that cannot be granted.
 *
 * Deliberately not `abs()`: wrapping a negative amount in abs() turned a misconfigured effect
 * expression into a silent *gain*, which is the worst of both worlds - the mistake is hidden
 * and its effect is inverted.
 */
private fun validatePetXpAmount(amount: Double, context: String): Boolean {
    if (!amount.isFinite() || amount <= 0.0) {
        plugin.logger.warning("Refused a non-positive xp grant of $amount for $context")
        return false
    }

    return true
}

/**
 * Give pet experience.
 *
 * XP only applies to a pet the player has actually adopted; see the config comment near the
 * top of config.yml for the decision this implements.
 */
fun Player.givePetExperience(pet: Pet, experience: Double, noMultiply: Boolean = false) {
    if (!this.hasPet(pet)) {
        return
    }

    val exp = if (noMultiply) experience else experience * this.petExperienceMultiplier

    if (!validatePetXpAmount(exp, "pet ${pet.id}")) {
        return
    }

    val gainEvent = PlayerPetExpGainEvent(this, pet, exp, !noMultiply)
    Bukkit.getPluginManager().callEvent(gainEvent)

    if (gainEvent.isCancelled) {
        return
    }

    this.giveExactPetExperience(pet, gainEvent.amount)
}

/**
 * Give exact pet experience, without calling PlayerPetExpGainEvent.
 *
 * Guarded the same as [givePetExperience]: this function's documented purpose is to skip the
 * gain event, and it is reachable directly from commands and the API.
 */
fun Player.giveExactPetExperience(pet: Pet, experience: Double) {
    if (!this.hasPet(pet)) {
        return
    }

    if (!validatePetXpAmount(experience, "pet ${pet.id}")) {
        return
    }

    val startLevel = this.getPetLevel(pet)
    val change = LevelProgression.progress(pet.curve, startLevel, this.getPetXP(pet), experience)

    if (change.stopReason == StopReason.INVALID_REQUIREMENT) {
        pet.warnBrokenCurveOnce(startLevel + 1)
    }

    this.setPetXP(pet, change.newXp)

    val gained = change.levelsGained ?: return

    this.setPetLevel(pet, change.newLevel)

    // One event per level crossed: a single grant spanning five levels must not swallow four
    // of the level-up rewards.
    for (level in gained) {
        Bukkit.getPluginManager().callEvent(PlayerPetLevelUpEvent(this, pet, level))
    }
}
