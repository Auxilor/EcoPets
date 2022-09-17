package com.willfp.ecopets.pets

import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.slot
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
    private const val pageKey = "page"

    private fun getPage(menu: Menu, player: Player): Int {
        val pages =
            ceil(Pets.values().filter { player.getPetLevel(it) > 0 }.size.toDouble() / petAreaSlots.size).toInt()

        val page = menu.getState(player, pageKey) ?: 1

        return max(min(pages, page + 1), 1)
    }

    @JvmStatic
    @ConfigUpdater
    fun update(plugin: EcoPetsPlugin) {
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

        menu = buildMenu(plugin)
    }

    private fun buildMenu(plugin: EcoPetsPlugin): Menu {
        val petInfoItemBuilder = { player: Player, _: Menu ->
            val pet = player.activePet

            if (pet == null) {
                ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.pet-info.no-active.item"))).setDisplayName(
                        plugin.configYml.getFormattedString("gui.pet-info.no-active.name")
                    ).addLoreLines(plugin.configYml.getFormattedStrings("gui.pet-info.no-active.lore")).build()
            } else {
                pet.getPetInfoIcon(player)
            }
        }

        val petIconBuilder = { player: Player, menu: Menu, index: Int ->
            val page = getPage(menu, player)

            val unlockedPets =
                Pets.values().sortedByDescending { player.getPetLevel(it) }.filter { player.getPetLevel(it) > 0 }

            val pagedIndex = ((page - 1) * petAreaSlots.size) + index

            val pet = unlockedPets.getOrNull(pagedIndex)
            pet?.getIcon(player) ?: ItemStack(Material.AIR)
        }

        return menu(plugin.configYml.getInt("gui.rows")) {
            setTitle(plugin.langYml.getString("menu.title"))

            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("gui.mask.materials")),
                    *plugin.configYml.getStrings("gui.mask.pattern").toTypedArray()
                )
            )

            setSlot(plugin.configYml.getInt("gui.pet-info.row"),
                plugin.configYml.getInt("gui.pet-info.column"),
                slot(petInfoItemBuilder) {
                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player
                        player.activePet?.levelGUI?.open(player)
                    }
                })

            for ((index, pair) in petAreaSlots.withIndex()) {
                val (row, column) = pair

                setSlot(row, column, slot({ p, m -> petIconBuilder(p, m, index) }) {
                    setUpdater { p, m, _ ->
                        petIconBuilder(p, m, index)
                    }

                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player

                        val page = getPage(menu, player)

                        val unlockedPets = Pets.values().sortedByDescending { player.getPetLevel(it) }
                            .filter { player.getPetLevel(it) > 0 }

                        val pagedIndex = ((page - 1) * petAreaSlots.size) + index

                        val pet = unlockedPets.getOrNull(pagedIndex) ?: return@onLeftClick

                        if (player.activePet == pet) {
                            player.activePet = null
                            player.playSound(
                                player.location,
                                Sound.valueOf(plugin.configYml.getString("gui.pet-icon.click.sound").uppercase()),
                                1f,
                                plugin.configYml.getDouble("gui.pet-icon.click.pitch").toFloat()
                            )
                            return@onLeftClick
                        }

                        player.activePet = pet
                        player.playSound(
                            player.location,
                            Sound.valueOf(plugin.configYml.getString("gui.pet-icon.click.sound").uppercase()),
                            1f,
                            plugin.configYml.getDouble("gui.pet-icon.click.pitch").toFloat()
                        )
                    }

                    onShiftLeftClick { event, _ ->
                        val player = event.whoClicked as Player
                        val page = getPage(menu, player)

                        val unlockedPets = Pets.values().sortedByDescending { player.getPetLevel(it) }
                            .filter { player.getPetLevel(it) > 0 }

                        val pagedIndex = ((page - 1) * petAreaSlots.size) + index

                        val pet = unlockedPets.getOrNull(pagedIndex) ?: return@onShiftLeftClick

                        val item = pet.inventoryItem(player, pet) ?: return@onShiftLeftClick

                        if (player.activePet == pet) {
                            player.activePet = null
                        }

                        if (player.activePet != pet) {
                            player.activePet = null
                        }

                        player.setPetLevel(pet, 0)
                        player.setPetXP(pet, 0.0)
                        DropQueue(player)
                            .addItem(item)
                            .forceTelekinesis()
                            .push()

                        player.playSound(
                            player.location,
                            Sound.valueOf(plugin.configYml.getString("gui.pet-icon.click.sound").uppercase()),
                            1f,
                            plugin.configYml.getDouble("gui.pet-icon.click.pitch").toFloat()
                        )
                    }
                })
            }

            setSlot(plugin.configYml.getInt("gui.prev-page.location.row"),
                plugin.configYml.getInt("gui.prev-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.prev-page.item"))).setDisplayName(
                        plugin.configYml.getString("gui.prev-page.name")
                    ).build()
                ) {
                    onLeftClick { event, _, menu ->
                        val player = event.whoClicked as Player
                        val page = getPage(menu, player)

                        val newPage = max(1, page - 1)

                        menu.addState(player, pageKey, newPage)
                    }
                })

            setSlot(plugin.configYml.getInt("gui.next-page.location.row"),
                plugin.configYml.getInt("gui.next-page.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.next-page.item"))).setDisplayName(
                        plugin.configYml.getString("gui.next-page.name")
                    ).build()
                ) {
                    onLeftClick { event, _, menu ->
                        val player = event.whoClicked as Player

                        val pages = ceil(Pets.values()
                            .filter { player.getPetLevel(it) > 0 }.size.toDouble() / petAreaSlots.size
                        ).toInt()

                        val page = getPage(menu, player)

                        val newPage = min(pages, page + 1)

                        menu.addState(player, pageKey, newPage)
                    }
                })

            setSlot(plugin.configYml.getInt("gui.close.location.row"),
                plugin.configYml.getInt("gui.close.location.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.close.item"))).setDisplayName(
                        plugin.configYml.getString(
                            "gui.close.name"
                        )
                    ).build()
                ) {
                    onLeftClick { event, _ -> event.whoClicked.closeInventory() }
                })

            if (plugin.configYml.getBool("gui.deactivate-pet.enabled")) {
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
            }
        }
    }

    @JvmStatic
    fun open(player: Player) {
        menu.open(player)
    }
}
