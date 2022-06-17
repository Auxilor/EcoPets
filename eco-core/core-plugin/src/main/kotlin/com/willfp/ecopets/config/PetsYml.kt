package com.willfp.ecopets.config

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.BaseConfig
import com.willfp.eco.core.config.ConfigType

class PetsYml(plugin: EcoPlugin) : BaseConfig("pets", plugin, false, ConfigType.YAML)
