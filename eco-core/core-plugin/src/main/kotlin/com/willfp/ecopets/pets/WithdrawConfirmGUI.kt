package com.willfp.ecopets.pets

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
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

            setMask(
                FillerMask(
                    MaskItems.fromItemNames(cfg.getStrings("mask.materials")),
                    *cfg.getStrings("mask.pattern").toTypedArray()
                )
            )

            setSlot(
                confirmCfg.getInt("row"),
                confirmCfg.getInt("column"),
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

            setSlot(
                cancelCfg.getInt("row"),
                cancelCfg.getInt("column"),
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

            for (config in cfg.getSubsections("custom-slots")) {
                setSlot(
                    config.getInt("row"),
                    config.getInt("column"),
                    ConfigSlot(config)
                )
            }
        }

        gui.open(player)
    }
}
