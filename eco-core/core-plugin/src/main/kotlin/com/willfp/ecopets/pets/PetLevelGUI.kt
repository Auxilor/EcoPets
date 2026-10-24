package com.willfp.ecopets.pets

import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.component.ConfigButton
import com.willfp.eco.core.gui.component.LevelComponent
import com.willfp.eco.core.gui.component.LevelState
import com.willfp.eco.core.gui.component.addComponent
import com.willfp.eco.core.gui.component.closeButton
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.NumberUtils
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

            override fun getLevelState(player: Player, level: Int): LevelState =
                LevelState.of(level, player.getPetLevel(pet))
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

            addComponent(
                ConfigButton(
                    plugin.configYml.getSubsection("level-gui.progression-slots.prev-page"),
                    defaultItem = "arrow",
                    layer = MenuLayer.LOWER
                ) { player, _, _, _ -> PetsGUI.open(player) }
            )

            addPageChanger(plugin.configYml, "level-gui.progression-slots.prev-page", PageChanger.Direction.BACKWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "level-gui.progression-slots.next-page", PageChanger.Direction.FORWARDS, pageChangeSound)

            addComponent(closeButton(plugin.configYml.getSubsection("level-gui.progression-slots.close")))

            addCustomSlots(plugin.configYml, "level-gui")
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
