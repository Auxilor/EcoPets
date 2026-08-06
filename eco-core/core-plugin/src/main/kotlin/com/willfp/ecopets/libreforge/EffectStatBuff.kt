package com.willfp.ecopets.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoskills.api.addStatModifier
import com.willfp.ecoskills.api.modifiers.ModifierOperation
import com.willfp.ecoskills.api.modifiers.StatModifier
import com.willfp.ecoskills.api.removeStatModifier
import com.willfp.ecoskills.stats.Stats
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.Identifiers
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class EffectStatBuff(
    id: String,
    private val statId: String
) : Effect<NoCompileData>(id) {
    override val arguments = arguments {
        require("amount", "You must specify the amount to buff!")
    }

    override fun onEnable(
        dispatcher: Dispatcher<*>,
        config: Config,
        identifiers: Identifiers,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ) {
        val player = dispatcher.dispatcher as? Player ?: return

        // Stats are configured in EcoSkills and may not exist on this server.
        val stat = Stats.get(statId)

        if (stat == null) {
            if (warnedMissingStats.add(statId)) {
                Bukkit.getLogger().warning(
                    "EcoPets: effect $id references EcoSkills stat '$statId', which is not configured - ignoring"
                )
            }
            return
        }

        // Optional operation - "add" (default) applies an additive modifier,
        // "multiply" applies a multiplicative modifier (e.g. amount 1.10 = +10%).
        val operation = when (config.getString("operation").lowercase()) {
            "multiply" -> ModifierOperation.MULTIPLY
            "", "add" -> ModifierOperation.ADD
            else -> {
                if (warnedBadOperation.add(id)) {
                    Bukkit.getLogger().warning(
                        "EcoPets: effect $id has invalid operation '${config.getString("operation")}' - " +
                            "must be 'add' or 'multiply', defaulting to 'add'"
                    )
                }
                ModifierOperation.ADD
            }
        }

        player.addStatModifier(
            StatModifier(
                identifiers.uuid,
                stat,
                config.getDoubleFromExpression("amount", player),
                operation
            )
        )
    }

    override fun onDisable(
        dispatcher: Dispatcher<*>,
        identifiers: Identifiers,
        holder: ProvidedHolder
    ) {
        val player = dispatcher.dispatcher as? Player ?: return

        player.removeStatModifier(identifiers.uuid)
    }

    private companion object {
        val warnedMissingStats = mutableSetOf<String>()
        val warnedBadOperation = mutableSetOf<String>()
    }
}
