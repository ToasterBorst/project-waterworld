# Changelog

## Unreleased

### Terrain — recovery
- Soft-fade absolute-Y density restored (no height compression); fade band **Y23→83**
- Simple ocean floor palette (warm → sand, cold/normal → gravel); removed climate dirt/sandstone accents and seabed material painter
- Restored ocean monument terrain-clear protection mixin

### Structures — seed-map fidelity
- Structure biome gates (`isValidBiome` + monument `getBiomesWithin`) sample vanilla overworld climate at Y≈63 so XZ matches seed maps
- Ruined portals: ocean-floor variant only, but biome gate includes all overworld portal biomes so seed-map land sites become submerged portals
- Shipwrecks: ocean variant only (beached omitted); spacing nudged to 22

- Buried treasure uses vanilla beach biomes (chests on ocean floor under beach columns)
- Pyramids/temples/trail ruins on seabed; hut boat + outpost ship substitutes; villages/igloos/mansions disabled
- Monument / portal / wreck spacing kept near vanilla (not the denser pre-freeze numbers)
- Monument guardians ignore wild guardian day delay / chance
- Mid-depth cave biomes no longer paint the seabed (dripstone/lush); caves kept only below Y=0

## 1.0.0 — Initial Release (unreleased)

### World Generation
- Flooded overworld with flat ocean surface at Y=112
- Natural seabed terrain below water level
- Vanilla biome distribution in the air column above water
- Land structures (villages, outposts, igloos, etc.) disabled

### Crafting
- Bamboo can substitute for sticks in any crafting recipe

### Mob Spawning
- Wild guardians spawn naturally in ocean biomes
- Drowned can spawn mounted on wild guardians (with trident chance)
- Drowned roam onto land instead of retreating to water
- Illagers spawn with difficulty-scaled armor during patrols and raids
- Pillager patrols spawn in bamboo rafts on the water (2 per raft)
- Raid mobs spawn in bamboo rafts; ravagers ride as passengers with a jockey pilot
- Wandering traders spawn in bamboo rafts with one llama passenger

### Boat AI
- Hostile mobs steer boats toward attack targets
- Pillagers load and fire crossbows from boats with vanilla-accurate state machine
- Melee raiders close distance and attack from boats
- Wandering traders approach nearby players and flee from threats
- Intelligent dismount: mobs exit boats near land when appropriate
- Remount behavior: mobs in water seek nearby boats to re-board
- Swim-to-land fallback for dismounted mobs in water

### Configuration
- All features individually toggleable via `config/waterworld.properties`
- Server-side only for joining — clients do not need the mod installed

---

### 2026-07-08

#### World Generation — `WaterworldBiomeSource` rework
- Sample vanilla overworld biomes first; substitute only when the vertical layer demands it
- Surface layer (block Y ≥ 108): beaches, stony shores, rivers, and mushroom fields kept verbatim where the vanilla seed places them; ocean-tagged biomes replaced with climate-matched inland biomes
- Underwater layer: genuine vanilla oceans and cave/underground biomes kept; vanilla-land columns replaced with climate-matched **non-deep** ocean biomes (keeps monuments and deep-ocean spawns out of land columns)
- Tag-driven classification (`minecraft:is_ocean`, `c:is_ocean`, `c:is_deep_ocean`, `c:is_cave`, `c:is_underground`) — future vanilla and modded biomes with correct tags work without code changes
- 4-block fuzz buffer: top water quart (Y 108–111) and everything above use surface-layer biomes so water tint, fog, and sky at sea level match the surface biome

#### Datapack — structures
- Buried treasure: `has_structure/buried_treasure` tag override uses `#minecraft:is_ocean` so chests generate on the ocean floor; treasure maps from shipwrecks and ocean ruins point at real chests
- Trail ruins: empty structure set disables generation (gate biomes exist only in the air column, so ruins would anchor to a nonexistent surface)

#### Mob Spawning — sea turtles
- Natural spawning in warm and lukewarm oceans on underwater sand (`turtle_ocean_spawns`, `turtle_spawn_weight` config keys)
- Spawn placement mixins allow turtle attempts on seabed sand; egg-laying still requires player-placed sand above water

#### Client — Mod Menu (optional, no runtime dependency)
- Mod icon, homepage, Mod Menu metadata (GitHub releases link, update checker), and `modmenu` entrypoint
- `WaterworldModMenu` + `WaterworldConfigScreen`: reflection-driven in-game config over `WaterworldConfig` fields
- Lang keys for Mod Menu summary and description
- `compileOnly` Mod Menu 20.0.0-beta.4 — mod runs fine without Mod Menu installed

#### Configuration
- Removed seven unwired `disable_*` structure booleans (never read by code); land structure disabling documented as handled by the built-in datapack
