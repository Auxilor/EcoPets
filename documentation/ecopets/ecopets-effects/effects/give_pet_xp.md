# `give_pet_xp`
:::infoRequires:
EcoPets
:::

:::dangerTriggered Effect
This effect requires a [Trigger](https://plugins.auxilor.io/effects/all-triggers) to activate.
:::

Gives experience points for a certain pet
# Effect Syntax
```yaml
- id: give_pet_xp
  args:
    amount: 100 # The amount of xp to give
    pet: ghost_wolf # The pet to give the xp for
  ...other config (eg triggers, filters, mutators, etc)
```