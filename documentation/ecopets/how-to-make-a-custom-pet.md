---
title: "How to Make a Pet"
sidebar_position: 1
---

Pets are upgradable **companions** that float around a player and grant **buffs** while active. Each pet is one config file that levels up as the player earns **pet XP** from the triggers you pick. This page covers building a pet from scratch, the structure of its config, and how to test it.

## Quick start

1. Open `/plugins/EcoPets/pets/`.
2. Copy `_example.yml` and rename it to your pet's ID, e.g. `tiger.yml`.
3. Set the `name`, `description`, `icon`, and `entity-texture`.
4. Set `xp-requirements` (or `xp-formula`) and `xp-gain-methods` so the pet can level up.
5. Add `effects` for the buffs the pet grants while active.
6. Run `/ecopets reload`.
7. Give yourself the pet with `/ecopets give <player> <id>`, then open `/pets` and activate it to confirm it works.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real pet. You can also organise pets into subfolders inside `pets/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the pet's ID. You use this ID in commands, effects, and placeholders. See the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system) for how IDs resolve across eco plugins.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the pet will not load.
:::

## The structure of a pet

A pet config breaks into a few distinct parts:

| Part | What it controls |
| --- | --- |
| **Display** | The name, description, icon, and the in-world pet entity |
| **Progression** | The XP needed per level and how the pet earns XP |
| **Placeholders and descriptions** | Custom placeholders and the effect/reward text shown in GUIs |
| **Level up** | Messages and effects fired when the pet levels up |
| **Effects** | The buffs the pet grants while active, plus activation conditions |
| **Spawn egg** | An optional craftable or giveable egg that unlocks the pet |

Here is one complete pet with every part in place:

```yaml
# === Display: name, icon, and in-world appearance ===
name: "&6Tiger" # Display name of the pet
description: "&8&oLevel up by dealing melee damage" # Shown in the pet GUIs
# Texture of the floating pet entity; use modelengine:id for ModelEngine
entity-texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0="
icon: player_head texture:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0= # Icon shown in GUIs

# === Progression: XP needed per level and how it is earned ===
xp-requirements: # XP to reach each level from level 1; list length is the max level
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
xp-gain-methods: # How the pet earns XP
  - id: melee_attack
    multiplier: 0.5 # Multiplies the value from the trigger; use "value" for a flat amount
    conditions: [ ]

# === Placeholders and descriptions: custom text shown in GUIs ===
level-placeholders: # Custom placeholders for descriptions; no % in the id, %level% is allowed in value
  - id: "damage_multiplier"
    value: "%level%"
effects-description: # Text shown by %effects%, keyed by the minimum level it shows from
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"
rewards-description: # Same as above, but shown by %rewards%
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"

# === Level up: messages and effects fired on level up ===
level-up-messages: # Sent on level up, keyed by the minimum level it shows from
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"
level-up-effects: # Effects run on level up; %level% is the level just reached
  - id: give_item
    args:
      items:
        - diamond
    every: 5 # Run every 5 levels
    require: '%level% = 5' # Only run at or above level 5

# === Effects: buffs granted while the pet is active ===
effects: # The buffs the pet grants while active; %level% is available
  - id: damage_multiplier
    args:
      multiplier: '%level% * 0.01 + 1'
    triggers:
      - melee_attack
conditions: [ ] # Conditions for the effects to run; %level% is available
activate-conditions: [ ] # Conditions required to activate the pet

# === Spawn egg: optional item that unlocks the pet ===
spawn-egg:
  enabled: true # Whether the pet has a spawn egg
  item: blaze_spawn_egg unbreaking:1 hide_enchants
  name: "&6Tiger&f Pet Spawn Egg"
  lore:
    - ""
    - "&8&oPlace on the ground to"
    - "&8&ounlock the &r&6Tiger&8&o pet!"
  craftable: false # Whether the egg can be crafted
  recipe: [ ]
  recipe-permission: ecopets.craft.tiger # Optional; permission needed to craft the egg
```

### Display

Sets the pet's name, description, GUI icon, and the texture of the floating entity.

```yaml
name: "&6Tiger" # Display name of the pet
description: "&8&oLevel up by dealing melee damage" # Shown in the pet GUIs
# Texture of the floating pet entity; use modelengine:id for ModelEngine
entity-texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0="
icon: player_head texture:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTA5NWZjYzFlM2Q3Y2JkMzUwZjE5YjM4OTQ5OGFiOGJiOTZjNjVhZDE4NWQzNDU5MjA2N2E3ZDAzM2FjNDhkZSJ9fX0= # Icon shown in GUIs
```

### Progression

Controls the XP needed for each level and how the pet earns that XP. Pick one of two ways to define level requirements: a fixed list, or an infinite formula.

```yaml
xp-requirements: # XP to reach each level from level 1; list length is the max level
  - 50
  - 125
  - 200
  - 300
  - 500
```

```yaml
xp-formula: (2 ^ %level%) * 25 # XP per level, where %level% is the level being calculated; see https://plugins.auxilor.io/all-plugins/math
max-level: 100 # Optional; with a formula there is no max level unless you set one
```

XP is earned through `xp-gain-methods`, each a trigger with a multiplier or flat value and optional conditions.

```yaml
xp-gain-methods:
  - id: melee_attack
    multiplier: 0.5 # Multiplies the value from the trigger; use "value" for a flat amount
    conditions: [ ]
```

:::info Multiplier vs value
`multiplier` scales the value the trigger produces (e.g. half the damage dealt), so bigger hits give more XP. `value` gives a fixed amount per trigger regardless of its value.
:::

### Placeholders and descriptions

Defines custom placeholders and the effect and reward text shown in the GUIs.

```yaml
level-placeholders: # Custom placeholders for descriptions; no % in the id, %level% is allowed in value
  - id: "damage_multiplier"
    value: "%level%"
effects-description: # Text shown by %effects%, keyed by the minimum level it shows from
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"
rewards-description: # Same as above, but shown by %rewards%
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"
```

### Level up

Sets the message a player sees and the effects that fire when the pet levels up.

```yaml
level-up-messages: # Sent on level up, keyed by the minimum level it shows from
  1:
    - "&8» &8Gives a &a+%damage_multiplier%%&8 bonus to"
    - "   &8melee damage"
level-up-effects: # Effects run on level up; %level% is the level just reached
  - id: give_item
    args:
      items:
        - diamond
    every: 5 # Run every 5 levels
    require: '%level% = 5' # Only run at or above level 5
```

### Effects

The buffs the pet grants while active, plus the conditions that gate the effects and activating the pet.

```yaml
effects: # The buffs the pet grants while active; %level% is available
  - id: damage_multiplier
    args:
      multiplier: '%level% * 0.01 + 1'
    triggers:
      - melee_attack
conditions: [ ] # Conditions for the effects to run; %level% is available
activate-conditions: [ ] # Conditions required to activate the pet
```

You can make a pet auto-deactivate when its `activate-conditions` stop being met in [Plugin Config](plugin-config).

:::danger Effects are their own system
Effects, conditions, filters, mutators, and triggers are a shared eco system with their own docs.

- [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect)
- [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain)
:::

### Spawn egg

An optional item that unlocks the pet when placed, which you can give out or make craftable.

```yaml
spawn-egg:
  enabled: true # Whether the pet has a spawn egg
  item: blaze_spawn_egg unbreaking:1 hide_enchants
  name: "&6Tiger&f Pet Spawn Egg"
  lore:
    - ""
    - "&8&oPlace on the ground to"
    - "&8&ounlock the &r&6Tiger&8&o pet!"
  craftable: false # Whether the egg can be crafted
  recipe: [ ]
  recipe-permission: ecopets.craft.tiger # Optional; permission needed to craft the egg
```

:::tip
We support shaped and shapeless recipes. Check out [Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes) for how to configure these.
:::

## Internal placeholders

These placeholders are available inside this pet's config (descriptions, messages, effect args).

| Placeholder | Value |
| --- | --- |
| `%level%` | The player's pet level. Useful for scaling effects |
| `%level_numeral%` | The player's pet level shown as numerals |
| `%level_x%` | The player's pet level, +/- a value, e.g. `%level_-1%` is the current level minus 1 |
| `%level_x_numeral%` | The player's pet level, +/- a value, shown as numerals |

:::tip Troubleshooting
- **Pet not loading?** The file name or an ID has capitals, spaces, or hyphens. Rename it to lowercase letters, numbers, and underscores only.
- **Pet never levels up?** No matching `xp-gain-methods` trigger, or its conditions never pass. Check the trigger id and clear any unmet conditions.
- **Pet does nothing while active?** The `effects` block is empty or its triggers never fire. Add an effect with a valid trigger.
- **Changes not showing in game?** You didn't reload. Run `/ecopets reload` after editing.
:::

<hr/>

## Where to go next

- **Configuring effects:** [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect) for the buffs your pet grants.
- **Plugin config:** [Plugin Config](plugin-config) to customise the GUIs and pet entity.
- **Default pets:** the shipped configs on [GitHub](https://github.com/Auxilor/EcoPets/tree/master/eco-core/core-plugin/src/main/resources/pets).
- **Community configs:** browse user-made pets on [lrcdb](https://lrcdb.auxilor.io/).