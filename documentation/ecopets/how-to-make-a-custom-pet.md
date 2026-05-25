---
title: How to make a Pet
sidebar_position: 1
---

## How to add pets
Each pet is its own config file, placed in the `/pets/` folder, and you can add or remove them as you please. There's an example config called `_example.yml` to help you out!

The ID of the Pet is the file name. This is what you use in commands, effects and placeholders.
ID's must be lowercase letters, numbers, and underscores only.

## Example Pet Config

```yaml
name: "&6Tiger"
description: "&8&oLevel up by dealing melee damage"
entity-texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0="
icon: player_head texture:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0=

xp-requirements:
  - 50
  - 125
  - 200
  - 300
  - 500
  - 750
  - 1000
  - 1500
  - 2000
  - 3500
  - 5000
  - 7500
  - 10000

xp-gain-methods:
  - id: melee_attack
    multiplier: 0.5
    conditions: [ ]

level-placeholders:
  - id: "damage_multiplier"
    value: "%level%"

effects-description:
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"

rewards-description:
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"

level-up-messages:
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"

level-up-effects:
  - id: give_item
    args:
      items:
        - diamond
    every: 5
    require: '%level% = 5'

effects:
  - id: damage_multiplier
    args:
      multiplier: '%level% * 0.01 + 1'
    triggers:
      - melee_attack

conditions: [ ]
activate-conditions: [ ]

spawn-egg:
  enabled: true
  item: blaze_spawn_egg unbreaking:1 hide_enchants
  name: "&6Tiger&f Pet Spawn Egg"
  lore:
    - ""
    - "&8&oPlace on the ground to"
    - "&8&ounlock the &r&6Tiger&8&o pet!"
  craftable: false
  recipe: [ ]
  recipe-permission: ecopets.craft.tiger
```

## Understanding all the sections

Below is a breakdown of all the sections in the pet config, and what they do.

### The Pet Info Section

```yaml
name: "&6Tiger" # The display name of the pet
description: "&8&oLevel up by dealing melee damage" # The description of the pet
# The texture of the pet entity in game
# If you're using modelengine, use modelengine:id as the texture
entity-texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0="
# The icon in GUIs
icon: player_head texture:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0=

```

### The Progression Section
#### XP Requirements

There are two ways to specify level XP requirements:
1. A formula to calculate for infinite levels

```yaml
xp-formula: (2 ^ %level%) * 25 # The formula to calculate XP requirements for each level, where %level% is the level to calculate for. See here for math https://plugins.auxilor.io/all-plugins/math
max-level: 100 # (Optional) The max level, if not specified, there is no max level  
```

2. A list of XP requirements for each level
```yaml
xp-requirements: # The XP required to reach each level, from Level 1. The length of the list is the max level.
- 50 # XP required to reach level 1
- 125 # XP required to reach level 2
- 200  
- 300  
- 500  
- 750  
- 1000  
- 1500  
- 2000  
- 3500  
- 5000  
- 7500  
- 10000
```
#### XP Gain Methods

```yaml
# An XP gain method takes a trigger, a multiplier, conditions, and filters.
# The 'multiplier' takes the value produced by the trigger and multiplies it
# Alternatively, you can use 'value' to count a specific number and not a multiplier
xp-gain-methods:
  - id: melee_attack
    multiplier: 0.5 # You can also use "value" here
    conditions: [ ]
```
:::tip

In xp-gain-methods, using `multiplier` means the XP gained is based on the value produced by the trigger. <br/>
Alternatively, using `value` means the XP gained is a specific number, regardless of the trigger's value.

:::

### The Additional Options Section

```yaml
# Custom placeholders to be used in descriptions,
# Don't add % to the IDs, this is done automatically
# The value takes a %level% placeholder and is a mathetmatical expression
level-placeholders:
  - id: "damage_multiplier"
    value: "%level%"

# The text shown with the %effects% placeholder
# The number dictates the minimum level for this text to show for
# Adding new levels will override this text on those levels and above
effects-description:
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"

# Same as above, but for %rewards%
rewards-description:
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"
```

### The Level Up Section
```yaml
# The message sent when the player levels up
level-up-messages:
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"

# Effects to run when the pet levels up  
# %level% is the level the pet leveled up to.  
# If you want to restrict this to certain levels, you can use  
# require: %level% = 20, or require: %level% < 50, etc.  
# If you want a reward to run every x levels, you can use  
# every: 1, or every: 12, etc  
level-up-effects:
  - id: give_item
    args:
      items:
        - diamond
    every: 5 # Gives the reward every 5 levels  
    require: '%level% = 5' # Requires level 5 before receiving rewards

# The effects for the pet, has %level% as a placeholder
effects:
  - id: damage_multiplier
    args:
      multiplier: '%level% * 0.01 + 1'
    triggers:
      - melee_attack
```

### The Effects Section
:::dangerEffects Section

The effects section is the core functionality of the pet. You can configure effects, conditions, filters, mutators and triggers in this section to run whilst the pet is active.

Check out [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect) to understand how to configure this section correctly.

For more advanced users or setups, you can configure chains in this section to string together different effects under one trigger. Check out [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain) for more info.

:::
```yaml
# The effects for the pet, has %level% as a placeholder
effects:
  - id: damage_multiplier
    args:
      multiplier: '%level% * 0.01 + 1'
    triggers:
      - melee_attack

# The conditions for the pet, also has %level% as a placeholder
conditions: [ ]
# The conditions required to activate the pet.
activate-conditions: [ ]
```
:::tip

You can configure if the pet should automatically deactivate when conditions aren't met in config.yml

:::

### The Spawn Egg

```yaml
spawn-egg:
  enabled: true # If the pet should have a spawn egg
  item: blaze_spawn_egg unbreaking:1 hide_enchants
  name: "&6Tiger&f Pet Spawn Egg"
  lore:
    - ""
    - "&8&oPlace on the ground to"
    - "&8&ounlock the &r&6Tiger&8&o pet!"
  craftable: false
  recipe: [ ]
  recipe-permission: ecopets.craft.tiger # (Optional) The permission required to craft this recipe.
```
:::tip

We support shaped and shapeless recipes. Check out [Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes) for more info on how to configure these.

:::
### Internal Placeholders

| Placeholder         | Value                                                                    |
|---------------------|--------------------------------------------------------------------------|
| `%level%`           | The player's pet level. Useful for creating scaling effects              |
| `%level_numeral%`   | The player's pet level shown as Numerals                                 |
| `%level_x%`         | The player's pet level, +/- a value. eg. `%level_-1%` is current level-1 |
| `%level_x_numeral%` | The player's pet level, +/- a value, shown as Numerals                   |

<hr/>

## Default configs
The default configs can be found [here](https://github.com/Auxilor/EcoPets/tree/master/eco-core/core-plugin/src/main/resources/pets). <br/>
You can find additional user-created configs on [lrcdb](https://lrcdb.auxilor.io/).
