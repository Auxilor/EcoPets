---
title: "Commands and Permissions"
sidebar_position: 4
---

Every EcoPets command and permission node, for giving, resetting, importing, and managing pets. Admin commands sit under `/ecopets`; player-facing ones under `/pets`.

| Command                                   | Description                                          | Permission                   |
|-------------------------------------------|------------------------------------------------------|------------------------------|
| `/ecopets give <player> <pet>`            | Give a pet                                           | `ecopets.command.give`       |
| `/ecopets giveegg <player> <pet> [level] [xp]` | Give a pet egg, optionally at a level/xp        | `ecopets.command.give`       |
| `/ecopets reset <player> <pet>`           | Reset a pet                                          | `ecopets.command.reset`      |
| `/ecopets givexp <player> <pet> <amount>` | Give xp to a pet                                     | `ecopets.command.givexp`     |
| `/pets`                                   | Open the pets menu                                   | `ecopets.command.pets`       |
| `/pets activate <pet>`                    | Activate a pet                                       | `ecopets.command.activate`   |
| `/pets deactivate`                        | Deactivate a pet                                     | `ecopets.command.deactivate` |
| `/ecopets import <id>`                    | Import a pet from [lrcdb](https://lrcdb.auxilor.io/) | `ecopets.command.import`     |
| `/ecopets export <id>`                    | Export a pet to [lrcdb](https://lrcdb.auxilor.io/)   | `ecopets.command.export`     |

### Additional permissions

| Permission                         | Description                                                                                         |
|------------------------------------|-----------------------------------------------------------------------------------------------------|
| `ecopets.xpmultiplier.<%increase>` | Multiply pet XP gain. The math is `1 + (<%increase> / 100)`. Example: `200` = 3x XP, `50` = 1.5x XP |
| `ecopets.xpmultiplier.50percent`   | Gives 50% more pet XP (1.5x multiplier)                                                             |
| `ecopets.xpmultiplier.double`      | Gives double pet XP (2x multiplier)                                                                 |
| `ecopets.xpmultiplier.triple`      | Gives triple pet XP (3x multiplier)                                                                 |
| `ecopets.xpmultiplier.quadruple`   | Gives quadruple pet XP (4x multiplier)                                                              |

<hr/>

## Where to go next

- **Make a pet:** [How to Make a Pet](how-to-make-a-custom-pet) to create the pets you give out.
- **Plugin config:** [Plugin Config](plugin-config) to customise the GUIs and pet entity.
- **Import and export:** browse or share pets on [lrcdb](https://lrcdb.auxilor.io/).
