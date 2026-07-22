# Waterworld

Inspired by Skyblock, except you're Kevin Costner. Waterworld. Get it? The world is water. Flooded. Higher sea level, natural seabed, vanilla (mods supported) biome distribution in the air column for all your building and farming needs. Lots of little tweaks for difficulty and fun, like wandering traders and pillager patrols on boats! Server-side, clients do not need it installed to join.

Fabric. Server-side only for multiplayer — clients do not need the mod. Installing the mod **client-side** (optionally with [Mod Menu](https://modrinth.com/mod/modmenu)) adds an in-game config screen; recommended for singleplayer and is not required for gameplay.


## Features

- **Waterworld** — Endless ocean at configurable sea level, default Y=101. Soft-faded vanilla seabed (~Y23–83), with vanilla features and subterrean biomes below. Surface biomes above water for the player to build into. Biome mods supported.
- **Boat AI** — Sentient mobs (pillagers, vindicators, evokers, witches, villagers, wandering traders) can pilot and exit boats/rafts. Sentient mobs might also open doors, whoopsie.
- **Wandering trader rafts** — Traders spawn in bamboo rafts with stacked llama passengers.
- **Ocean pillager patrols** — Pillager patrols spawn in bamboo rafts on the water.
- **Raid mobs in rafts** — Raiders spawn in bamboo rafts over water, ravagers in back seats with jockeys.
- **Armored illagers** — Illagers spawn with armor during patrols, raids, and outposts. Chance and tier ramp with world age and game difficulty.
- **Wild guardians** — Guardians spawn naturally in ocean biomes at low frequency.
- **Drowned guardian riders** — Wild guardians can spawn with mounted drowned (trident chance).
- **Drowned on land** — Drowned will chase/roam onto land.
- **Sea turtles** — Turtles spawn naturally on warm/lukewarm/deep lukewarm ocean seabed sand.
- **Treasure maps** — Buried treasure generates on the ocean floor.
- **Structures** — Villages, mansions, igloos disabled; desert pyramids, jungle temples, and trail ruins generate flooded on the seabed; swamp huts / outposts are custom boat/ship substitutes.
- **Mod Menu integration** — [Mod Menu](https://modrinth.com/mod/modmenu) for Fabric is supported.

All features are individually toggleable via `config/waterworld.properties` (or the in-game config screen with Mod Menu).


## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft/mod version.
2. Download jar and place it in your `mods/` folder.
3. Launch the game, create a new world with world type **Waterworld**

For dedicated servers, set in `server.properties`:

```properties
level-type=project-waterworld:waterworld
```


## Configuration

Config file: `config/waterworld.properties` (created on first launch). Most toggles apply on next world load. Spawn weights (`guardian_spawn_weight`, `turtle_spawn_weight`) and drowned spawn costs require a **full game restart**. Land structures (villages, igloos, mansions) are disabled by the mod datapack for every overworld while this mod is installed.

### Activation

| Key | Default | Effect |
|-----|---------|--------|
| `activation_mode` | `auto` | `auto` = gameplay only in Waterworld worlds; `always` = all worlds; `never` = disabled (datapack structure overrides still apply) |

### Player Spawn

| Key | Default | Effect |
|-----|---------|--------|
| `spawn_ocean_biome` | *(empty)* | Force spawn in a specific ocean biome (`warm_ocean`, `lukewarm_ocean`, `deep_ocean`, etc.). Empty = disabled |
| `spawn_island` | `false` | Generate a small island at world spawn |
| `spawn_gear` | `true` | Give players a bamboo chest raft with starter items on first spawn |
| `sea_level` | `101` | Ocean waterline for ships/biomes. For new worlds, keep `data/project-waterworld/worldgen/noise_settings/waterworld.json` `sea_level` equal to this value |

### Guardians & Drowned

| Key | Default | Effect |
|-----|---------|--------|
| `wild_guardian_spawns` | `true` | Wild guardians spawn in ocean biomes outside monuments (**restart** for weight changes) |
| `guardian_spawn_weight` | `1` | Spawn weight for wild guardians (squid is ~5; keep very low) (**restart**) |
| `guardian_spawn_chance` | `0.01` | Chance a guardian spawn attempt succeeds (0.0–1.0) |
| `guardian_min_days` | `5` | Days before wild guardians start spawning |
| `guardian_full_strength_days` | `20` | Day guardian spawn chance reaches full configured value |
| `drowned_ride_guardians` | `true` | Drowned riders on wild guardians, with trident chance |
| `drowned_rider_chance` | `0.85` | Chance a wild guardian spawns with a drowned rider (0.0–1.0) |
| `mounted_guardian_speed_factor` | `0.55` | Guardian swim speed while a drowned is riding (0.1–1.0; 1.0 = off). Bare guardians unchanged |
| `drowned_rider_min_days` | `5` | Days before drowned riders appear on guardians |
| `drowned_rider_full_strength_days` | `18` | Day drowned rider chance reaches full configured value |
| `trident_rider_chance` | `0.20` | Chance a drowned rider carries a trident (0.0–1.0) |
| `trident_drowned_min_days` | `10` | Days before drowned can spawn with tridents (0 = immediate) |
| `drowned_can_go_on_land` | `true` | Drowned can roam on land instead of returning to water |
| `drowned_spawn_charge` | `0.15` | Vanilla `spawn_costs` charge for ocean drowned (0 = off; higher = sparser) (**restart**) |
| `drowned_spawn_energy_budget` | `0.7` | Energy budget paired with drowned spawn charge (**restart**) |

### Turtles

| Key | Default | Effect |
|-----|---------|--------|
| `turtle_ocean_spawns` | `true` | Turtles spawn in biomes tagged `#project-waterworld:turtle_spawns` |
| `turtle_spawn_weight` | `5` | Spawn weight for ocean turtles (**restart**) |

### Illagers & Patrols

| Key | Default | Effect |
|-----|---------|--------|
| `ocean_pillager_patrols` | `true` | Pillager patrols and raids spawn in boats on water |
| `patrol_min_days` | `5` | Days before pillager patrols begin (also starts armor ramp) |
| `patrol_full_strength_days` | `24` | Day patrol frequency and armor reach full strength |
| `pillager_armor` | `true` | Illagers spawn with armor during patrols, raids, and outposts |
| `pillager_armor_chance` | `0.3` | Base chance per armor piece (0.0–1.0); scaled by difficulty and world age |
| `armor_scales_with_difficulty` | `true` | Armor chance/tier scales with game difficulty and ramps with world age |

### Wandering Traders

| Key | Default | Effect |
|-----|---------|--------|
| `wandering_trader_boats` | `true` | Wandering traders spawn in boats at sea with llamas |
| `wandering_trader_min_days` | `2` | Days before wandering traders appear |
| `wandering_trader_full_strength_days` | `12` | Day wandering trader frequency reaches full rate |

### Boat Behavior

| Key | Default | Effect |
|-----|---------|--------|
| `mobs_can_exit_boats` | `true` | Intelligent mobs can exit boats when they reach land |
| `mobs_can_pilot_boats` | `true` | Illagers and wandering traders can steer boats |

## License

CC0-1.0 (see [LICENSE](LICENSE))
