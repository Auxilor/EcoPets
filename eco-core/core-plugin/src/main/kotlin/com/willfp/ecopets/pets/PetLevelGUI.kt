package com.willfp.ecopets.pets

import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.NumberUtils
import com.willfp.ecomponent.components.LevelComponent
import com.willfp.ecomponent.components.LevelState
import com.willfp.ecopets.plugin
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PetLevelGUI(
    private val pet: Pet
) {
    private val menu: Menu

    init {
        val maskPattern = plugin.configYml.getStrings("level-gui.mask.pattern").toTypedArray()
        val maskItems = MaskItems.fromItemNames(plugin.configYml.getStrings("level-gui.mask.materials"))

        val progressionPattern = plugin.configYml.getStrings("level-gui.progression-slots.pattern")

        val component = object : LevelComponent() {
            override val pattern: List<String> = progressionPattern
            override val maxLevel: Int = pet.maxLevel

            override fun getLevelItem(player: Player, menu: Menu, level: Int, levelState: LevelState): ItemStack {
                val key = levelState.name.lowercase().replace("_", "-")

                val prefix = if (player.getPetLevel(pet) == pet.maxLevel) "max-level-" else ""

                return ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.$key.item")))
                    .setDisplayName(
                        plugin.configYml.getFormattedString("level-gui.progression-slots.$key.name")
                            .replace("%pet%", pet.name)
                            .replace("%level%", level.toString())
                            .replace("%level_numeral%", NumberUtils.toNumeral(level))
                    )
                    .addLoreLines(
                        pet.injectPlaceholdersInto(
                            plugin.configYml.getFormattedStrings("level-gui.progression-slots.$key.${prefix}lore"),
                            player,
                            forceLevel = level
                        )
                    )
                    .setAmount(
                        if (plugin.configYml.getBool("level-gui.progression-slots.level-as-amount")) level else 1
                    )
                    .build()
            }

            override fun getLevelState(player: Player, level: Int): LevelState {
                return when {
                    level <= player.getPetLevel(pet) -> LevelState.UNLOCKED
                    level == player.getPetLevel(pet) + 1 -> LevelState.IN_PROGRESS
                    else -> LevelState.LOCKED
                }
            }
        }

        fun pageButtonItem(basePath: String, state: String): ItemStack? {
            val itemString = if (state == "active") {
                plugin.configYml.getStringOrNull("$basePath.item")
                    ?: plugin.configYml.getStringOrNull("$basePath.material")
            } else {
                plugin.configYml.getStringOrNull("$basePath.item-inactive")
                    ?: plugin.configYml.getStringOrNull("$basePath.material-inactive")
            } ?: return null

            // Deprecated: use the item/item-inactive keys to set the name instead
            val name = if (state == "active") {
                plugin.configYml.getStringOrNull("$basePath.name")
            } else {
                plugin.configYml.getStringOrNull("$basePath.name-inactive")
            }

            val builder = ItemStackBuilder(Items.lookup(itemString))

            if (name != null) {
                builder.setDisplayName(name)
            }

            return builder.build()
        }

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("level-gui.progression-slots.page-change-sound"))

        menu = menu(plugin.configYml.getInt("level-gui.rows")) {
            title = plugin.langYml.getString("menu.level-title").takeIf { it.isNotEmpty() } ?: run {
                plugin.langYml.set("menu.level-title", "%pet% (%page%/%max_page%)")
                plugin.langYml.save()
                plugin.langYml.getString("menu.level-title")
            }

            title = title.replace("%pet%", pet.name)

            maxPages(component.pages)

            setMask(
                FillerMask(
                    maskItems,
                    *maskPattern
                )
            )

            addComponent(1, 1, component)

            addComponent(
                MenuLayer.LOWER,
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.column"),
                slot(
                    pageButtonItem("level-gui.progression-slots.prev-page", "active")
                        ?: ItemStackBuilder(Items.lookup("arrow")).build()
                ) {
                    onLeftClick { player, _, _, _ -> PetsGUI.open(player) }
                }
            )

            pageButtonItem("level-gui.progression-slots.prev-page", "active")?.let { active ->
                addPageChanger(
                    PageChanger.Direction.BACKWARDS,
                    active,
                    null,
                    pageChangeSound,
                    plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.row"),
                    plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.column")
                )
            }

            pageButtonItem("level-gui.progression-slots.next-page", "active")?.let { active ->
                addPageChanger(
                    PageChanger.Direction.FORWARDS,
                    active,
                    pageButtonItem("level-gui.progression-slots.next-page", "inactive"),
                    pageChangeSound,
                    plugin.configYml.getInt("level-gui.progression-slots.next-page.location.row"),
                    plugin.configYml.getInt("level-gui.progression-slots.next-page.location.column")
                )
            }

            val closeEnabled = plugin.configYml.getBoolOrNull("level-gui.progression-slots.close.enabled") ?: true
            if (closeEnabled) {
                setSlot(
                    plugin.configYml.getInt("level-gui.progression-slots.close.location.row"),
                    plugin.configYml.getInt("level-gui.progression-slots.close.location.column"),
                    slot(
                        ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.close.material")))
                            .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.close.name"))
                            .build()
                    ) {
                        onLeftClick { event, _ ->
                            event.whoClicked.closeInventory()
                        }
                    }
                )
            }

            for (config in plugin.configYml.getSubsections("level-gui.custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
