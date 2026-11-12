package com.willfp.ecopets.items

import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.petEggKey
import com.willfp.ecopets.pets.petEggLevel
import com.willfp.ecopets.pets.petEggLevelKey
import com.willfp.ecopets.pets.petEggXpKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import wiki.ecohub.game.item.getPersistentData
import wiki.ecohub.game.item.lookup.LookupArgParser
import java.util.function.Predicate

object ArgParserPetLevel : LookupArgParser {
    override fun parseArguments(args: Array<out String>, item: ItemStack): Predicate<ItemStack>? {
        val arg = args.firstOrNull { it.startsWith("pet-level:") } ?: return null
        val level = arg.substringAfter(":").toIntOrNull() ?: return null

        item.editMeta { meta ->
            meta.persistentDataContainer.set(petEggLevelKey, PersistentDataType.INTEGER, level)

            val pdc = meta.persistentDataContainer
            val petId = pdc.get(petEggKey, PersistentDataType.STRING)
            val pet = petId?.let { Pets.getByID(it) }
            if (pet != null) {
                val xp = pdc.get(petEggXpKey, PersistentDataType.DOUBLE) ?: 0.0
                pet.makeSpawnEgg(level, xp)?.itemMeta?.also { eggMeta ->
                    meta.displayName(eggMeta.displayName())
                    meta.lore(eggMeta.lore())
                }
            }
        }

        return Predicate { it.petEggLevel == level }
    }

    override fun serializeBack(item: ItemStack): String? {
        val level = item.getPersistentData(petEggLevelKey, PersistentDataType.INTEGER) ?: return null
        return "pet-level:$level"
    }
}
