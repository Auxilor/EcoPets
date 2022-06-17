package com.willfp.ecopets.pets

import com.willfp.eco.core.config.updating.ConfigUpdater
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
        for (column in topLeftColumn..bottomRightColumn) {
            for (row in topLeftRow..bottomRightRow) {
                petAreaSlots.add(Pair(row, column))
            }
        }

        menu = buildMenu(plugin)
    }

    private fun buildMenu(plugin: EcoPetsPlugin): Menu {
        val petInfoItemBuilder = { player: Player, _: Menu ->
            val pet = player.activePet

            if (pet == null) {
                ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.pet-info.no-active.item")))
                    .setDisplayName(plugin.configYml.getFormattedString("gui.pet-info.no-active.name"))
                    .addLoreLines(plugin.configYml.getFormattedStrings("gui.pet-info.no-active.lore"))
                    .build()
            } else {
                pet.getPetInfoIcon(player)
            }
        }

        return menu(plugin.configYml.getInt("gui.rows")) {
            setTitle(plugin.langYml.getString("menu.title"))

            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("gui.mask.materials")),
                    *plugin.configYml.getStrings("gui.mask.pattern").toTypedArray()
                )
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

            for ((index, pair) in petAreaSlots.withIndex()) {
                val (row, column) = pair

                setSlot(row, column, slot(ItemStack(Material.AIR)) {
                    setUpdater { player, _, _ ->
                        val unlockedPets = Pets.values().filter { player.getPetLevel(it) > 0 }
                        val pet = unlockedPets.getOrNull(index)
                        pet?.getIcon(player) ?: ItemStack(Material.AIR)
                    }

                    onLeftClick { event, _, _ ->
                        val player = event.whoClicked as Player
                        val unlockedPets = Pets.values()
                            .sortedByDescending { player.getPetLevel(it) }
                            .filter { player.getPetLevel(it) > 0 }

                        val pet = unlockedPets.getOrNull(index) ?: return@onLeftClick

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
        }
    }

    @JvmStatic
    fun open(player: Player) {
        menu.open(player)
    }
}
