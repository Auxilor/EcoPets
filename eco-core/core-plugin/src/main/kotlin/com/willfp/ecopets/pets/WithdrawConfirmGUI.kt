package com.willfp.ecopets.pets

import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecopets.plugin
import org.bukkit.entity.Player

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
