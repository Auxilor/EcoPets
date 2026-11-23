package com.willfp.ecopets.pets

import com.willfp.ecopets.plugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.builder.ItemStackBuilder
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.addPageChanger
import wiki.ecohub.modules.menus.component.PagedAreaComponent
import wiki.ecohub.modules.menus.component.addComponent
import wiki.ecohub.modules.menus.component.addPagedArea
import wiki.ecohub.modules.menus.component.closeButton
import wiki.ecohub.modules.menus.getSlotLocations
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.page.PageChanger
import wiki.ecohub.modules.menus.setMask
import wiki.ecohub.modules.menus.slot
import wiki.ecohub.modules.menus.slot.Slot
import wiki.ecohub.modules.menus.slot.SlotArea
import wiki.ecohub.modules.menus.slot.getSlotArea
import wiki.ecohub.ui.text.StringUtils
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PetsGUI {
    @Volatile
    private lateinit var menu: Menu

    private val lastToggleTimes = ConcurrentHashMap<UUID, Long>()

    internal fun update() {
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

        return menu(plugin.configYml.getInt("gui.rows")) {
            setTitle(plugin.langYml.getString("menu.title"))

            setMask(plugin.configYml, "gui")

            onRender { player, menu ->
                menu.setState(player, "ecopets", true)
            }

            addPagedArea(PetAreaComponent(plugin.configYml.getSlotArea("gui.pet-area")))

            addPageChanger(plugin.configYml, "gui.prev-page", PageChanger.Direction.BACKWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "gui.next-page", PageChanger.Direction.FORWARDS, pageChangeSound)

            for (location in plugin.configYml.getSlotLocations("gui.pet-info")) {
                setSlot(
                    row = location.row,
                    column = location.column,
                    slot = slot(petInfoItemBuilder) {
                        onLeftClick { event, _, _ ->
                            val player = event.whoClicked as Player
                            player.activePet?.levelGUI?.open(player)
                        }
                    }
                )
            }

            addComponent(closeButton(plugin.configYml.getSubsection("gui.close")))

            for (location in plugin.configYml.getSlotLocations("gui.deactivate-pet")) {
                setSlot(
                    row = location.row,
                    column = location.column,
                    slot = slot(
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

            val withdrawEnabled = plugin.configYml.getBoolOrNull("gui.withdraw-pet.enabled") ?: true
            if (withdrawEnabled) {
                val confirmEnabled = plugin.configYml.getBoolOrNull("gui.withdraw-pet.confirm.enabled") ?: true
                for (location in plugin.configYml.getSlotLocations("gui.withdraw-pet")) {
                    setSlot(
                        row = location.row,
                        column = location.column,
                        slot = slot(
                            { player: Player, _: Menu ->
                                ItemStackBuilder(Items.lookup(plugin.configYml.getString("gui.withdraw-pet.item")))
                                    .setDisplayName(plugin.configYml.getFormattedString("gui.withdraw-pet.name"))
                                    .addLoreLines {
                                        val pet = player.activePet
                                        plugin.configYml.getStrings("gui.withdraw-pet.lore").map { line ->
                                            line.replace(
                                                "%withdraw_price%",
                                                pet?.withdrawPrice?.getDisplay(player) ?: ""
                                            )
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
                                if (confirmEnabled) {
                                    WithdrawConfirmGUI.open(player, pet)
                                } else {
                                    player.closeInventory()
                                    withdrawPet(player, pet)
                                }
                            }
                        }
                    )
                }
            }

            for (location in plugin.configYml.getSlotLocations("gui.toggle")) {
                setSlot(
                    row = location.row,
                    column = location.column,
                    slot = slot(togglePetItemBuilder) {
                        onLeftClick { event, _ ->
                            val player = event.whoClicked as Player

                            // Toggling respawns/removes the pet entity, so rate-limit to prevent spam-click lag.
                            val now = System.currentTimeMillis()
                            val cooldown = plugin.configYml.getIntOrNull("gui.toggle.cooldown") ?: 500
                            val lastToggle = lastToggleTimes[player.uniqueId]
                            if (lastToggle != null && now - lastToggle < cooldown) {
                                return@onLeftClick
                            }
                            lastToggleTimes[player.uniqueId] = now

                            player.shouldHidePet = !player.shouldHidePet
                        }
                    }
                )
            }

            addCustomSlots(plugin.configYml, "gui")
        }
    }

    @JvmStatic
    fun open(player: Player) {
        menu.open(player)
    }
}

private class PetAreaComponent(area: SlotArea) : PagedAreaComponent<Pet>(area) {
    override fun getEntries(player: Player, menu: Menu): List<Pet> = Pets.values()
        .sortedByDescending { player.getPetLevel(it) }
        .filter { player.getPetLevel(it) > 0 }

    override fun getEntrySlot(player: Player, menu: Menu, entry: Pet): Slot =
        slot({ viewer, _ -> entry.getIcon(viewer) }) {
            onLeftClick { event, _, _ ->
                val clicker = event.whoClicked as Player

                if (clicker.activePet != entry && !entry.canActivate(clicker)) {
                    clicker.sendMessage(
                        plugin.langYml.getMessage("cannot-activate-pet", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                            .replace(oldValue = "%pet%", newValue = entry.name)
                    )
                } else {
                    if (clicker.activePet != entry) {
                        clicker.activePet = entry
                    }
                    PlayableSound.create(plugin.configYml.getSubsection("gui.pet-icon.click"))?.playTo(clicker)
                }
            }
        }

    override fun getEmptySlot(player: Player, menu: Menu): Slot = slot(ItemStack(Material.AIR))
}
