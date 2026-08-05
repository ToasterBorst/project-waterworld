# Waterworld

Inspired by Skyblock, except you're Kevin Costner. Waterworld. Get it? The world is water. Flooded. Higher sea level, natural seabed, vanilla (mods supported) biome distribution in the air column for all your building and farming needs. Lots of little tweaks for difficulty and fun, like wandering traders and pillager patrols on boats! Server-side, clients do not need it installed to join.

Fabric. Server-side only for multiplayer — clients do not need the mod. Installing the mod **client-side** (optionally with [Mod Menu](https://modrinth.com/mod/modmenu)) adds an in-game config screen; recommended for singleplayer and is not required for gameplay.


## Features

- **Waterworld** — Endless ocean at configurable sea level, default Y=101. Soft-faded vanilla seabed (~Y23–83), with vanilla features and subterranean biomes below. Surface biomes in the air column (dipping a few blocks into the water for clean tint); ocean biomes below. Biome mods supported.
- **Boat AI** — Sentient mobs (pillagers, vindicators, evokers, witches, wandering traders) can pilot and exit boats/rafts (always on when Waterworld is active). Sentient mobs might also open doors, whoopsie.
- **Wandering trader rafts** — Traders spawn in bamboo rafts with stacked llama passengers.
- **Ocean pillager patrols** — Pillager patrols spawn in bamboo rafts on the water.
- **Raid mobs in rafts** — Raiders spawn in bamboo rafts over water, ravagers in back seats with jockeys.
- **Wild guardians** — Guardians spawn naturally in ocean biomes at low frequency.
- **Drowned guardian riders** — Wild guardians can spawn with mounted baby drowned (no tridents).
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
level-type=waterworld:waterworld
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
| `spawn_island` | `false` | Easy mode: generate a small island at world spawn (and at each Spawn Party origin when that mod is present) |
| `spawn_gear` | `true` | Give players a bamboo chest raft with starter items on first spawn (island not required) |
| `spawn_gear_items` | `minecraft:bamboo,minecraft:fishing_rod` | Comma-separated item ids for the raft chest; optional `:count` (default 1). Empty = empty chest |
| `sea_level` | `101` | Ocean waterline for ships/biomes. For new worlds, keep `data/waterworld/worldgen/noise_settings/waterworld.json` `sea_level` equal to this value |

**Spawn Party (optional):** Waterworld does **not** depend on Spawn Party. Alone, gear still grants on JOIN for new players; `spawn_island` builds one island at **world spawn**.

With Spawn Party loaded:
- World-spawn island is **skipped**; islands are generated **once per party** at that party’s origin when the first member is placed
- Each player still gets spawn gear (raft) on their first-origin placement if `spawn_gear` is on
- Soft `suggests` only — Spawn Party calls into Waterworld after placement


### Guardians & Drowned

| Key | Default | Effect |
|-----|---------|--------|
| `wild_guardian_spawns` | `true` | Wild guardians spawn in ocean biomes outside monuments (**restart** for weight changes) |
| `guardian_spawn_weight` | `1` | Spawn weight for wild guardians (squid is ~5; keep very low) (**restart**) |
| `guardian_spawn_chance` | `0.01` | Chance a guardian spawn attempt succeeds (0.0–1.0) |
| `guardian_min_days` | `5` | Days before wild guardians start spawning |
| `guardian_full_strength_days` | `20` | Day guardian spawn chance reaches full configured value |
| `drowned_ride_guardians` | `true` | Baby drowned riders on wild guardians (no tridents) |
| `drowned_rider_chance` | `0.05` | Chance a wild guardian spawns with a drowned rider (0.0–1.0) |
| `mounted_guardian_speed_factor` | `0.55` | Guardian swim speed while a drowned is riding (0.1–1.0; 1.0 = off). Bare guardians unchanged |
| `drowned_rider_min_days` | `5` | Days before drowned riders appear on guardians |
| `drowned_rider_full_strength_days` | `18` | Day drowned rider chance reaches full configured value |
| `trident_drowned_min_days` | `10` | Days before drowned can spawn with tridents (0 = immediate) |
| `drowned_can_go_on_land` | `true` | Drowned can roam on land instead of returning to water |
| `drowned_spawn_charge` | `0.15` | Vanilla `spawn_costs` charge for ocean drowned (0 = off; higher = sparser) (**restart**) |
| `drowned_spawn_energy_budget` | `0.7` | Energy budget paired with drowned spawn charge (**restart**) |

### Turtles

| Key | Default | Effect |
|-----|---------|--------|
| `turtle_ocean_spawns` | `true` | Turtles spawn in biomes tagged `#waterworld:turtle_spawns` |
| `turtle_spawn_weight` | `5` | Spawn weight for ocean turtles (**restart**) |

### Illagers & Patrols

| Key | Default | Effect |
|-----|---------|--------|
| `ocean_pillager_patrols` | `true` | Pillager patrols and raid waves spawn in boats on water |
| `patrol_min_days` | `5` | Days before pillager patrols begin |
| `patrol_full_strength_days` | `24` | Day patrol frequency reaches full strength |

### Wandering Traders

| Key | Default | Effect |
|-----|---------|--------|
| `wandering_trader_boats` | `true` | Wandering traders spawn in boats at sea with llamas |
| `wandering_trader_min_days` | `2` | Days before wandering traders appear |
| `wandering_trader_full_strength_days` | `12` | Day wandering trader frequency reaches full rate |

## License

CC0-1.0 (see [LICENSE](LICENSE))
