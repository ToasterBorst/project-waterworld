# Project Waterworld

Fabric mod for Minecraft **26.1.2** that generates a true water world: a flat ocean surface at **Y=112**, natural seabed below, and vanilla overworld biome distribution in the air column above the water so players can build and get normal farming, mob, and feature behavior.

Server-side only — clients do not need the mod installed to join.

## Features

- **Waterworld terrain** — Endless ocean with configurable sea level, vanilla biomes above water
- **Bamboo as sticks** — Bamboo can substitute for sticks in any crafting recipe
- **Wild guardian spawns** — Guardians spawn naturally in ocean biomes at low frequency
- **Drowned guardian riders** — Wild guardians can spawn with mounted drowned (trident chance)
- **Drowned on land** — Drowned roam onto land instead of retreating to water
- **Armored illagers** — Illagers spawn with armor during patrols and raids, scaling with difficulty
- **Ocean pillager patrols** — Pillager patrols spawn in bamboo rafts on the water
- **Raid mobs in rafts** — Raiders spawn in bamboo rafts over water, ravagers in back seats with jockeys
- **Wandering trader rafts** — Traders spawn in bamboo rafts with one llama passenger
- **Boat AI** — Illagers steer toward targets and attack from boats; traders approach players and flee threats; intelligent mobs dismount on land
- **Land structure removal** — Villages, outposts, igloos, and other land structures disabled

All features are individually toggleable via `config/project-waterworld.json`.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.18.4+** for Minecraft **26.1.2**
2. Download `project-waterworld-1.0.0.jar` and place it in your `mods/` folder
3. Launch the game, create a new world with world type **Waterworld**

For dedicated servers, set in `server.properties`:

```properties
level-type=project-waterworld:waterworld
```

## Configuration

On first launch, a config file is generated at `config/project-waterworld.json` with all defaults. Edit it to toggle features or tune spawn rates.

## Documentation

| Doc | Purpose |
|-----|---------|
| [docs/VERSIONS.md](docs/VERSIONS.md) | Pinned MC / Fabric / Java versions |
| [docs/TESTING.md](docs/TESTING.md) | Development setup, building from source, and testing |
| [docs/DESIGN.md](docs/DESIGN.md) | Goals and acceptance criteria |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Datapack vs mod split |

## License

CC0-1.0 (see [LICENSE](LICENSE))
