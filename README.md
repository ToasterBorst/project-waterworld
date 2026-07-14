# Waterworld

Fabric mod for Minecraft **26.2** that generates a true water world: a flat ocean surface at **Y=112**, natural seabed below, and vanilla overworld biome distribution in the air column above the water so players can build and get normal farming, mob, and feature behavior.

Inspired by Skyblock, except you're Kevin Costner. Waterworld. Get it? The world is water. Flooded.

Server-side only for joining multiplayer — clients do not need the mod installed to connect. Installing the mod **client-side** (optionally with [Mod Menu](https://modrinth.com/mod/modmenu)) adds an in-game config screen; it is not required for gameplay.

## Features

- **Waterworld terrain** — Endless ocean at sea level Y=112, vanilla biomes above water (beaches, stony shores, rivers, and mushroom fields appear where the seed places them)
- **Bamboo as sticks** — Bamboo can substitute for sticks in any crafting recipe
- **Wild guardian spawns** — Guardians spawn naturally in ocean biomes at low frequency
- **Drowned guardian riders** — Wild guardians can spawn with mounted drowned (trident chance)
- **Drowned on land** — Drowned roam onto land instead of retreating to water
- **Armored illagers** — Illagers spawn with armor during patrols and raids, scaling with difficulty
- **Ocean pillager patrols** — Pillager patrols spawn in bamboo rafts on the water
- **Raid mobs in rafts** — Raiders spawn in bamboo rafts over water, ravagers in back seats with jockeys
- **Wandering trader rafts** — Traders spawn in bamboo rafts with one llama passenger
- **Boat AI** — Illagers steer toward targets and attack from boats; traders approach players and flee threats; intelligent mobs dismount on land
- **Sea turtles** — Turtles spawn naturally on warm/lukewarm ocean seabed sand
- **Treasure maps** — Buried treasure generates on the ocean floor so shipwreck/ruin maps point at real chests (Heart of the Sea / conduit progression)
- **Land structure removal** — Villages, mansions, igloos disabled; desert pyramids, jungle temples, and trail ruins generate flooded on the seabed; swamp huts / outposts are boat/ship substitutes
- **Mod Menu integration** — Mod icon, description, and links when Mod Menu is installed client-side; in-game config screen for all toggles
- **Mod icon** — Shown in the mod list when installed client-side

All features are individually toggleable via `config/waterworld.properties` (or the in-game config screen with Mod Menu).

## Known trade-offs

- **No natural passive land animals** — Cow, sheep, pig, and chicken herd spawning needs grass present at chunk generation; the waterworld has none at gen time. Player-built grass platforms in the sky biomes **do** enable passive spawning afterward (the friendly mob cap starts empty), so ranching is intended progression.
- **No natural villages** — Villager trading and raid mechanics require curing zombie villagers or transporting villagers from elsewhere. Raids work with a player-built village (one villager plus a claimed bed).
- **Mending** — Not available from villagers; obtainable via fishing (treasure loot).

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.19.3+** for Minecraft **26.2**
2. Download `waterworld-26.2-1.0.0.jar` and place it in your `mods/` folder
3. Launch the game, create a new world with world type **Waterworld**

For dedicated servers, set in `server.properties`:

```properties
level-type=project-waterworld:waterworld
```

Optional: install [Mod Menu](https://modrinth.com/mod/modmenu) on the client for an in-game settings screen (compile-only integration — the mod runs fine without it).

## Configuration

On first launch, a config file is generated at `config/waterworld.properties` with commented defaults grouped by feature (activation, spawn, guardians, turtles, illagers, traders, boats, crafting). Edit it to toggle features or tune spawn rates. With Mod Menu installed client-side, the same options are available from the mod list (activation mode is a cycle toggle: Auto / Always / Never).

Legacy `config/project-waterworld.properties` and `config/project-waterworld.json` are migrated automatically on load.

## Documentation

| Doc | Purpose |
|-----|---------|
| [docs/VERSIONS.md](docs/VERSIONS.md) | Pinned MC / Fabric / Java versions |
| [docs/TESTING.md](docs/TESTING.md) | Development setup, building from source, and testing |
| [docs/DESIGN.md](docs/DESIGN.md) | Goals and acceptance criteria |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Datapack vs mod split |

## License

CC0-1.0 (see [LICENSE](LICENSE))
