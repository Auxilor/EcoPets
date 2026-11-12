package com.willfp.ecopets.pets

import com.willfp.ecopets.plugin
import org.bukkit.entity.Player
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.builder.ItemStackBuilder
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.getSlotLocations
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.setMask
import wiki.ecohub.modules.menus.slot

object WithdrawConfirmGUI {
    fun open(player: Player, pet: Pet) {
        val cfg = plugin.configYml.getSubsection("gui.withdraw-pet.confirm")
        val confirmCfg = cfg.getSubsection("confirm")
        val cancelCfg = cfg.getSubsection("cancel")

        val gui = menu(cfg.getInt("rows")) {
            title = cfg.getFormattedString("title")

            setMask(cfg)

            for (location in confirmCfg.getSlotLocations()) {
                setSlot(
                    location.row,
                    location.column,
                    slot(
                        ItemStackBuilder(Items.lookup(confirmCfg.getString("item")))
                            .setDisplayName(confirmCfg.getFormattedString("name"))
                            .addLoreLines(confirmCfg.getFormattedStrings("lore"))
                            .build()
                    ) {
                        onLeftClick { event, _ ->
                            val clicker = event.whoClicked as Player
                            PlayableSound.create(confirmCfg.getSubsection("sound"))?.playTo(clicker)
                            clicker.closeInventory()
                            withdrawPet(clicker, pet)
                        }
                    }
                )
            }

            for (location in cancelCfg.getSlotLocations()) {
                setSlot(
                    location.row,
                    location.column,
                    slot(
                        ItemStackBuilder(Items.lookup(cancelCfg.getString("item")))
                            .setDisplayName(cancelCfg.getFormattedString("name"))
                            .addLoreLines(cancelCfg.getFormattedStrings("lore"))
                            .build()
                    ) {
                        onLeftClick { event, _ ->
                            val clicker = event.whoClicked as Player
                            PlayableSound.create(cancelCfg.getSubsection("sound"))?.playTo(clicker)
                            PetsGUI.open(clicker)
                        }
                    }
                )
            }

            addCustomSlots(cfg, "")
        }

        gui.open(player)
    }
}
