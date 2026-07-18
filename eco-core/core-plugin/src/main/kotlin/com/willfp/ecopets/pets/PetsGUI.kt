package com.willfp.ecopets.pets

import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.Page
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.StringUtils
import com.willfp.ecopets.plugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

object PetsGUI {
    private lateinit var menu: Menu
    private val petAreaSlots = mutableListOf<Pair<Int, Int>>()

    internal fun update() {
        val topLeftRow = plugin.configYml.getInt("gui.pet-area.top-left.row")
        val topLeftColumn = plugin.configYml.getInt("gui.pet-area.top-left.column")
        val bottomRightRow = plugin.configYml.getInt("gui.pet-area.bottom-right.row")
        val bottomRightColumn = plugin.configYml.getInt("gui.pet-area.bottom-right.column")

        petAreaSlots.clear()
        for (row in topLeftRow..bottomRightRow) {
            for (column in topLeftColumn..bottomRightColumn) {
                petAreaSlots.add(Pair(row, column))
            }
        }

        menu = buildMenu()
    }

    private fun buildMenu(): Menu {
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

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("gui.page-change-sound"))

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
                            if (!pet.canActivate(player)) {
                                player.sendMessage(
                                    plugin.langYml.getMessage("cannot-activate-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                                        .replace("%pet%", pet.name)
                                )
                                return@onLeftClick
                            }
                            player.activePet = pet
                        }
                        PlayableSound.create(plugin.configYml.getSubsection("gui.pet-icon.click"))?.playTo(player)
                    }
                })
            }

            addPageChanger(plugin.configYml, "gui.prev-page", PageChanger.Direction.BACKWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "gui.next-page", PageChanger.Direction.FORWARDS, pageChangeSound)

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

            val closeEnabled = plugin.configYml.getBoolOrNull("gui.close.enabled") ?: true
            if (closeEnabled) {
                setSlot(
                    plugin.configYml.getInt("gui.close.location.row"),
                    plugin.configYml.getInt("gui.close.location.column"),
                    slot(
                        ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.close.item")))
                            .setDisplayName(plugin.configYml.getString("gui.close.name"))
                            .build()
                    ) {
                        onLeftClick { event, _ -> event.whoClicked.closeInventory() }
                    }
                )
            }

            setSlot(
                plugin.configYml.getInt("gui.deactivate-pet.location.row"),
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

            val withdrawEnabled = plugin.configYml.getBoolOrNull("gui.withdraw-pet.enabled") ?: true
            if (withdrawEnabled) {
                setSlot(
                    plugin.configYml.getInt("gui.withdraw-pet.location.row"),
                    plugin.configYml.getInt("gui.withdraw-pet.location.column"),
                    slot(
                        { player: Player, _: Menu ->
                            ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.withdraw-pet.item")))
                                .setDisplayName(plugin.configYml.getFormattedString("gui.withdraw-pet.name"))
                                .addLoreLines {
                                    val pet = player.activePet
                                    plugin.configYml.getStrings("gui.withdraw-pet.lore").map { line ->
                                        line.replace("%withdraw_price%", pet?.withdrawPrice?.getDisplay(player) ?: "")
                                    }
                                }
                                .build()
                        }
                    ) {
                        onLeftClick { event, _ ->
                            val player = event.whoClicked as Player
                            val pet = player.activePet
                            val result = canWithdraw(player, pet)
                            if (result != WithdrawResult.OK || pet == null) {
                                result.notifyFail(player, pet)
                                return@onLeftClick
                            }
                            WithdrawConfirmGUI.open(player, pet)
                        }
                    }
                )
            }

            setSlot(
                plugin.configYml.getInt("gui.toggle.location.row"),
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
