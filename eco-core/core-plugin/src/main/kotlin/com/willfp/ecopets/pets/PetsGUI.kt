package com.willfp.ecopets.pets

import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.Page
import com.willfp.eco.core.gui.page.PageChangeEvent
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.ecopets.EcoPetsPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object PetsGUI {
    private lateinit var menu: Menu
    private val petAreaSlots = mutableListOf<Pair<Int, Int>>()

    @JvmStatic
    @ConfigUpdater
    fun update(plugin: EcoPetsPlugin) {
        val topLeftRow = plugin.configYml.getInt("gui.pet-area.top-left.row")
        val topLeftColumn = plugin.configYml.getInt("gui.pet-area.top-left.column")
        val bottomRightRow = plugin.configYml.getInt("gui.pet-area.bottom-right.row")
        val bottomRightColumn = plugin.configYml.getInt("gui.pet-area.bottom-right.column")

        petAreaSlots.clear()
        for (row in topLeftRow .. bottomRightRow) {
            for (column in topLeftColumn .. bottomRightColumn) {
                petAreaSlots.add(Pair(row, column))
            }
        }

        menu = buildMenu(plugin)
    }

    private fun buildMenu(plugin: EcoPetsPlugin): Menu {
        val petInfoItemBuilder = { player: Player, _: Menu ->
            val pet = player.activePet

            pet?.getPetInfoIcon(player)
                ?: ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.pet-info.no-active.item")))
                    .setDisplayName(plugin.configYml.getFormattedString("gui.pet-info.no-active.name"))
                    .addLoreLines(plugin.configYml.getFormattedStrings("gui.pet-info.no-active.lore"))
                    .build()
        }

        val togglePetItemBuilder = { player: Player, _: Menu ->
            val isPetVisible = !player.shouldHidePet

            if (isPetVisible) {
                ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.toggle.hide-pet.item")))
                    .setDisplayName(plugin.configYml.getFormattedString("gui.toggle.hide-pet.name"))
                    .addLoreLines(plugin.configYml.getFormattedStrings("gui.toggle.hide-pet.lore"))
                    .build()
            } else {
                ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.toggle.show-pet.item")))
                    .setDisplayName(plugin.configYml.getFormattedString("gui.toggle.show-pet.name"))
                    .addLoreLines(plugin.configYml.getFormattedStrings("gui.toggle.show-pet.lore"))
                    .build()
            }
        }

        val petIconBuilder = { player: Player, menu: Menu, index: Int ->
            val page = menu.getPage(player)

            val unlockedPets = Pets.values()
                .sortedByDescending { player.getPetLevel(it) }
                .filter { player.getPetLevel(it) > 0 }

            val pagedIndex = page * petAreaSlots.size - petAreaSlots.size + index

            val pet = unlockedPets.getOrNull(pagedIndex)
            pet?.getIcon(player) ?: ItemStack(Material.AIR)
        }

        return menu(plugin.configYml.getInt("gui.rows")) {
            title = plugin.langYml.getString("menu.title")

            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("gui.mask.materials")),
                    *plugin.configYml.getStrings("gui.mask.pattern").toTypedArray()
                )
            )

            onRender { player, menu ->
                menu.setState(player, "ecopets", true)

                if (menu.getPage(player) > menu.getMaxPage(player)) {
                    menu.setState(player, Page.PAGE_KEY, 1)
                }
            }

            for ((index, pair) in petAreaSlots.withIndex()) {
                val (row, column) = pair
                setSlot(row, column, slot({ player, menu -> petIconBuilder(player, menu, index) }) {
                    setUpdater { player, menu, _ ->
                        petIconBuilder(player, menu, index)
                    }

                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player
                        val page = menu.getPage(player)

                        val unlockedPets = Pets.values()
                            .sortedByDescending { player.getPetLevel(it) }
                            .filter { player.getPetLevel(it) > 0 }

                        val pagedIndex = page * petAreaSlots.size - petAreaSlots.size + index

                        val pet = unlockedPets.getOrNull(pagedIndex) ?: return@onLeftClick

                        if (player.activePet != pet) {
                            player.activePet = pet
                        }

                        player.playSound(
                            player.location,
                            Sound.valueOf(plugin.configYml.getString("gui.pet-icon.click.sound").uppercase()),
                            1f,
                            plugin.configYml.getDouble("gui.pet-icon.click.pitch").toFloat()
                        )
                    }
                })
            }

            // I do this for backwards compatibility because with PageChanger if you don't have any more pages, the item will disappear and this would require an update of the config for all users
            // This is terrible imo, but everything for compatibility!
            setSlot(
                plugin.configYml.getInt("gui.prev-page.location.row"),
                plugin.configYml.getInt("gui.prev-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.prev-page.item")))
                        .setDisplayName(plugin.configYml.getString("gui.prev-page.name"))
                        .build()
                )
                {
                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player
                        val page = menu.getPage(player)
                        val newPage = max(1, min(page + -1, menu.getMaxPage(player)))

                        if (newPage == page) {
                            return@onLeftClick
                        }

                        menu.setState(player, Page.PAGE_KEY, newPage)
                        menu.callEvent(player, PageChangeEvent(newPage, page))
                    }
                }
            )

            setSlot(
                plugin.configYml.getInt("gui.next-page.location.row"),
                plugin.configYml.getInt("gui.next-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.next-page.item")))
                        .setDisplayName(plugin.configYml.getString("gui.next-page.name"))
                        .build()
                )
                {
                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player
                        val page = menu.getPage(player)
                        val newPage = max(1, min(page + 1, menu.getMaxPage(player)))

                        if (newPage == page) {
                            return@onLeftClick
                        }

                        menu.setState(player, Page.PAGE_KEY, newPage)
                        menu.callEvent(player, PageChangeEvent(newPage, page))
                    }
                }
            )

            setSlot(
                plugin.configYml.getInt("gui.pet-info.row"),
                plugin.configYml.getInt("gui.pet-info.column"),
                slot(petInfoItemBuilder) {
                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player
                        player.activePet?.levelGUI?.open(player)
                    }
                }
            )

            setSlot(plugin.configYml.getInt("gui.close.location.row"),
                plugin.configYml.getInt("gui.close.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.close.item")))
                        .setDisplayName(plugin.configYml.getString("gui.close.name"))
                        .build()
                ) {
                    onLeftClick { event, _ -> event.whoClicked.closeInventory() }
                }
            )

            setSlot(plugin.configYml.getInt("gui.deactivate-pet.location.row"),
                plugin.configYml.getInt("gui.deactivate-pet.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.deactivate-pet.item")))
                        .setDisplayName(plugin.configYml.getString("gui.deactivate-pet.name"))
                        .build()
                ) {
                    onLeftClick { event, _ ->
                        val player = event.whoClicked as Player
                        player.activePet = null
                    }
                }
            )

            setSlot(plugin.configYml.getInt("gui.toggle.location.row"),
                plugin.configYml.getInt("gui.toggle.location.column"),
                slot(togglePetItemBuilder) {
                    onLeftClick { event, _ ->
                        val player = event.whoClicked as Player
                        player.shouldHidePet = !player.shouldHidePet
                    }
                }
            )

            for (config in plugin.configYml.getSubsections("gui.custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }

            maxPages { player ->
                val unlockedPets = Pets.values()
                    .sortedByDescending { player.getPetLevel(it) }
                    .filter { player.getPetLevel(it) > 0 }

                val perPage = petAreaSlots.size

                val pages = if (unlockedPets.isEmpty()) {
                    0
                } else {
                    ceil((unlockedPets.size.toDouble() / perPage)).toInt()
                }
                pages
            }
        }
    }

    @JvmStatic
    fun open(player: Player) {
        menu.open(player)
    }
}
