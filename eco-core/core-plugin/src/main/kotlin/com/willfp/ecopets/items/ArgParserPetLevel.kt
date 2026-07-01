package com.willfp.ecopets.items

import com.willfp.eco.core.items.args.LookupArgParser
import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.petEggKey
import com.willfp.ecopets.pets.petEggLevel
import com.willfp.ecopets.pets.petEggLevelKey
import com.willfp.ecopets.pets.petEggXpKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.function.Predicate

object ArgParserPetLevel : LookupArgParser {
    override fun parseArguments(args: Array<out String>, meta: ItemMeta): Predicate<ItemStack>? {
        val arg = args.firstOrNull { it.startsWith("pet-level:") } ?: return null
        val level = arg.substringAfter(":").toIntOrNull() ?: return null

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

        return Predicate { it.petEggLevel == level }
    }

    override fun serializeBack(meta: ItemMeta): String? {
        val level = meta.persistentDataContainer.get(petEggLevelKey, PersistentDataType.INTEGER) ?: return null
        return "pet-level:$level"
    }
}
