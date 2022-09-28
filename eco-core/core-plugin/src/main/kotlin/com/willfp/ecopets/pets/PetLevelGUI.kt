package com.willfp.ecopets.pets

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.util.NumberUtils
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class PetLevelGUI(
    plugin: EcoPlugin,
    private val pet: Pet
) {
    private val menu: Menu
    private val pageKey = "page"
    private var levelsPerPage by Delegates.notNull<Int>()
    private var pages by Delegates.notNull<Int>()

    private fun getPage(menu: Menu, player: Player): Int {
        val page = menu.getState(player, pageKey) ?: 1

        return min(pages, max(page, 1))
    }

    init {
        val maskPattern = plugin.configYml.getStrings("level-gui.mask.pattern").toTypedArray()
        val maskItems = MaskItems.fromItemNames(plugin.configYml.getStrings("level-gui.mask.materials"))

        val progressionOrder = "123456789abcdefghijklmnopqrstuvwxyz"
        val progressionPattern = plugin.configYml.getStrings("level-gui.progression-slots.pattern")

        val progressionSlots = mutableMapOf<Int, Pair<Int, Int>>()

        var x = 0
        for (row in progressionPattern) {
            x++
            var y = 0
            for (char in row) {
                y++
                if (char == '0') {
                    continue
                }

                val pos = progressionOrder.indexOf(char)

                if (pos == -1) {
                    continue
                }

                progressionSlots[pos + 1] = Pair(x, y)
            }
        }

        levelsPerPage = progressionSlots.size
        pages = ceil(pet.maxLevel.toDouble() / levelsPerPage).toInt()

        menu = menu(plugin.configYml.getInt("level-gui.rows")) {
            setTitle(pet.name)
            setMask(
                FillerMask(
                    maskItems,
                    *maskPattern
                )
            )

            for ((level, value) in progressionSlots) {
                setSlot(
                    value.first,
                    value.second,
                    slot(ItemStack(Material.BLACK_STAINED_GLASS_PANE)) {
                        setUpdater { player, menu, _ ->
                            val page = getPage(menu, player)

                            val slotLevel = ((page - 1) * levelsPerPage) + level

                            fun getItem(section: String) =
                                ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.$section.item")))
                                    .setDisplayName(
                                        plugin.configYml.getFormattedString("level-gui.progression-slots.$section.name")
                                            .replace("%pet%", pet.name)
                                            .replace("%level%", slotLevel.toString())
                                            .replace("%level_numeral%", NumberUtils.toNumeral(slotLevel))
                                    )
                                    .addLoreLines(
                                        pet.injectPlaceholdersInto(
                                            plugin.configYml.getFormattedStrings("level-gui.progression-slots.$section.lore"),
                                            player,
                                            forceLevel = slotLevel
                                        )
                                    )
                                    .build()

                            if (slotLevel > pet.maxLevel) {
                                maskItems.items[0].item
                            } else {
                                val item = when {
                                    slotLevel <= player.getPetLevel(pet) -> {
                                        getItem("unlocked")
                                    }
                                    slotLevel == player.getPetLevel(pet) + 1 -> {
                                        getItem("in-progress")
                                    }
                                    else -> {
                                        getItem("locked")
                                    }
                                }

                                if (plugin.configYml.getBool("level-gui.progression-slots.level-as-amount")) {
                                    item.amount = slotLevel
                                }
                                item
                            }
                        }
                    }
                )
            }
            setSlot(
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.prev-page.material")))
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.prev-page.name"))
                        .build()
                ) {
                    onLeftClick { event, _, menu ->
                        val player = event.whoClicked as Player
                        val page = getPage(menu, player)

                        val newPage = max(0, page - 1)

                        if (newPage == 0) {
                            PetsGUI.open(player)
                        } else {
                            menu.addState(player, pageKey, newPage)
                        }
                    }
                }
            )
            setSlot(
                plugin.configYml.getInt("level-gui.progression-slots.next-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.next-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("level-gui.progression-slots.next-page.material")))
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.next-page.name"))
                        .build()
                ) {
                    onLeftClick { event, _, menu ->
                        val player = event.whoClicked as Player

                        val page = getPage(menu, player)

                        val newPage = min(pages, page + 1)

                        menu.addState(player, pageKey, newPage)
                    }
                }
            )
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

            for (config in plugin.configYml.getSubsections("level-gui.custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }
        }
    }

    fun open(player: Player) = menu.open(player)
}
