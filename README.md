# Waterworld

Inspired by Skyblock, except you're Kevin Costner. Waterworld. Get it? The world is water. Flooded. Higher sea level, natural seabed, vanilla (mods supported) biome distribution in the air column for all your building and farming needs. Lots of little tweaks for difficulty and fun, like wandering traders and pillager patrols on boats! Server-side, clients do not need it installed to join.

Fabric. Server-side only for multiplayer — clients do not need the mod. Installing the mod **client-side** (optionally with [Mod Menu](https://modrinth.com/mod/modmenu)) adds an in-game config screen; recommended for singleplayer and is not required for gameplay.


## Features

- **Waterworld** — Endless ocean at configurable sea level, default Y=101. Soft-faded vanilla seabed (~Y23–83), with vanilla features and subterrean biomes below. Surface biomes above water for the player to build into. Biome mods supported.
- **Boat AI** — Sentient mobs (pillagers, villagers, traders) can pilot and exit boats/rafts.
- **Wandering trader rafts** — Traders spawn in bamboo rafts with stacked llama passengers.
- **Ocean pillager patrols** — Pillager patrols spawn in bamboo rafts on the water.
- **Raid mobs in rafts** — Raiders spawn in bamboo rafts over water, ravagers in back seats with jockeys.
- **Armored illagers** — Illagers spawn with armor during patrols and raids, scaling with difficulty.
- **Wild guardians** — Guardians spawn naturally in ocean biomes at low frequency.
- **Drowned guardian riders** — Wild guardians can spawn with mounted drowned (trident chance).
- **Drowned on land** — Drowned will chase/roam onto land.
- **Sea turtles** — Turtles spawn naturally on warm/lukewarm ocean seabed sand.
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



## License

CC0-1.0 (see [LICENSE](LICENSE))
