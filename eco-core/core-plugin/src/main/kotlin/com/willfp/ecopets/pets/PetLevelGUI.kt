package com.willfp.ecopets.pets

import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
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

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("level-gui.progression-slots.page-change-sound"))

        menu = menu(plugin.configYml.getInt("level-gui.rows")) {
            title = plugin.langYml.getString("menu.level-title").takeIf { it.isNotEmpty() } ?: run {
                plugin.langYml.set("menu.level-title", "%pet% (%page%/%max_page%)")
                plugin.langYml.save()
                plugin.langYml.getString("menu.level-title")
            }

            title = title.replace("%pet%", pet.name)

            maxPages(component.pages)

            setMask(plugin.configYml, "level-gui")

            addComponent(1, 1, component)

            for (location in plugin.configYml.getSlotLocations("level-gui.progression-slots.prev-page")) {
                addComponent(
                    MenuLayer.LOWER,
                    location.row,
                    location.column,
                    slot(
                        plugin.configYml.getStringOrNull("level-gui.progression-slots.prev-page.item")
                            ?.let { Items.lookup(it).item }
                            ?: ItemStackBuilder(Items.lookup("arrow")).build()
                    ) {
                        onLeftClick { player, _, _, _ -> PetsGUI.open(player) }
                    }
                )
            }

            addPageChanger(plugin.configYml, "level-gui.progression-slots.prev-page", PageChanger.Direction.BACKWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "level-gui.progression-slots.next-page", PageChanger.Direction.FORWARDS, pageChangeSound)

            val closeEnabled = plugin.configYml.getBoolOrNull("level-gui.progression-slots.close.enabled") ?: true
            if (closeEnabled) {
                for (location in plugin.configYml.getSlotLocations("level-gui.progression-slots.close")) {
                    setSlot(
                        location.row,
                        location.column,
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
            }

            for (config in plugin.configYml.getSubsections("level-gui.custom-slots")) {
                for (location in config.getSlotLocations()) {
                    setSlot(location.row, location.column, ConfigSlot(config))
                }
            }
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
