package com.willfp.ecopets.pets.entity

import wiki.ecohub.core.plugin.BaseEcoPlugin
import wiki.ecohub.provider.ExternalProvider

/**
 * Registers `<prefix>:<id>` pet entities drawn by the model plugin [integrationId].
 */
internal class ModelPetEntityProvider(
    private val prefix: String,
    private val integrationId: String
) : ExternalProvider {
    override fun getID(): String = prefix

    override fun onLoad(plugin: BaseEcoPlugin) {
        PetEntity.registerPetEntity(prefix) { pet, texture ->
            ModelPetEntity(pet, integrationId, texture)
        }
    }
}
