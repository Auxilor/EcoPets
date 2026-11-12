package com.willfp.ecopets.libreforge

import com.willfp.ecopets.api.event.PetEvent
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.filter.Filter
import wiki.ecohub.rules.trigger.TriggerData

object FilterPet : Filter<NoCompileData, Collection<String>>("pet") {
    override val description = "Matches when the pet involved in the trigger is one of the given pet IDs."

    override val categories = setOf("player")

    override val valueType = ArgType.STRING_LIST

    override val additionalInfo = listOf(
        "Does not match if the trigger data's event is not pet-related."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val event = data.event as? PetEvent ?: return false

        return value.any { petName ->
            petName.equals(event.pet.id, ignoreCase = true)
        }
    }
}
