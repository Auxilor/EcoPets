package com.willfp.ecopets.items

import com.willfp.ecopets.pets.Pets
import com.willfp.ecopets.pets.petEggKey
import com.willfp.ecopets.pets.petEggLevelKey
import com.willfp.ecopets.pets.petEggXp
import com.willfp.ecopets.pets.petEggXpKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import wiki.ecohub.game.item.lookup.LookupArgParser
import java.util.function.Predicate

object ArgParserPetXp : LookupArgParser {
    override fun parseArguments(args: Array<out String>, item: ItemStack): Predicate<ItemStack>? {
        val arg = args.firstOrNull { it.startsWith("pet-xp:") } ?: return null
        val xp = arg.substringAfter(":").toDoubleOrNull() ?: return null

        item.editMeta { meta ->
            meta.persistentDataContainer.set(petEggXpKey, PersistentDataType.DOUBLE, xp)

            val pdc = meta.persistentDataContainer
            val petId = pdc.get(petEggKey, PersistentDataType.STRING)
            val pet = petId?.let { Pets.getByID(it) }
            if (pet != null) {
                val level = pdc.get(petEggLevelKey, PersistentDataType.INTEGER) ?: 1
                pet.makeSpawnEgg(level, xp)?.itemMeta?.also { eggMeta ->
                    meta.displayName(eggMeta.displayName())
                    meta.lore(eggMeta.lore())
                }
            }
        }

        return Predicate { it.petEggXp == xp }
    }

    override fun serializeBack(item: ItemStack): String? {
        val xp = item.persistentDataContainer.get(petEggXpKey, PersistentDataType.DOUBLE) ?: return null
        return "pet-xp:$xp"
    }
}
