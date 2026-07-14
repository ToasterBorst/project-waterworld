# Testing Waterworld

Step-by-step guide to build, run, and verify the mod locally.

## 1. One-time setup

1. Install **Java 25** for the IDE (Gradle can auto-provision JDK 25 for builds via Foojay).
2. Optional: IntelliJ **2025.3+** or Cursor/VS Code with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack).

## 2. Build the mod

From the repository root in PowerShell (prefix scripts with `.\`):

```powershell
.\gradlew.bat build
```

Success produces: `build\libs\waterworld-26.2-1.0.0.jar`

If dependencies fail:

```powershell
.\gradlew.bat build --refresh-dependencies
```

## 3. Run in development (fastest loop)

```powershell
.\run-client.bat
```

Same as `.\gradlew.bat runClient`. First launch downloads Minecraft **26.2** and Fabric (several minutes). Versions come from [`gradle.properties`](../gradle.properties)—no separate Fabric installer needed for dev.

## 4. Fix IDE errors before editing Java

If Cursor shows errors on `net.minecraft.*` imports:

```powershell
.\gradlew.bat genSources vscode
```

Then: Command palette → **Java: Clean Java Language Server Workspace** → Reload window.

Confirm compile: `.\gradlew.bat build` should succeed even when the IDE is red.

## 5. Create a test world

1. Main menu → **Singleplayer** → **Create New World**
2. **World** tab → **World Type** → **Waterworld**
3. Note the **seed** for regression
4. **Creative** first to inspect terrain, then **Survival** for spawn

Suggested seeds:

| Seed | Notes |
|------|--------|
| `waterworld-dev-1` | General layout |
| `-422159142` | Random stress |

## 6. In-game verification checklist

| Check | How |
|-------|-----|
| Sea surface at Y=112 | Fly at Y=112; F3: water; no stone through surface |
| Air above surface | Y=113–160: air only |
| Seabed depth | Vanilla deep oceans preserved; land columns soft-faded into seabed hills ~**Y48–76** |
| Biomes above water | F3 in air (Y>112): vanilla overworld biomes; beaches, stony shores, rivers, mushroom fields where seed places them |
| Biomes at surface fuzz | F3 at Y=108–111: surface-layer biomes (matches sky column, not underwater ocean) |
| Biomes at/below surface | F3 at Y&lt;108: oceans (and caves only below Y=0); land/cave columns in the water band → non-deep ocean |
| Sea-level water appearance | Swim at Y=112: water tint/fog/sky match surface biome (not underwater ocean) |
| Datapack active | `/datapack list` — `project-waterworld` enabled |
| Mod loaded | `logs/latest.log` — `Initializing Waterworld` |
| No floating structures | Fly around surface; no villages/outposts/igloos above water |
| Buried treasure under beaches (seabed) | See [Buried treasure / treasure maps](#buried-treasure--treasure-maps) |
| Trail ruins disabled | See [Trail ruins](#trail-ruins) |
| Sea turtles | See [Sea turtles](#sea-turtles) |
| Mod Menu (optional) | See [Mod Menu client UI](#mod-menu-client-ui) |

## 7. Testing mob spawns

All spawn features are controlled by `config/waterworld.properties`. Delete the file to regenerate defaults.

### Ocean monuments (density + guardians)

Monument XZ should match vanilla seed maps (same salt/spacing; biome surround uses vanilla overworld climate, not remapped underwater oceans). Compare with a seed-map tool, then:

```
/locate structure minecraft:monument
```

Inside a monument, guardians (and elders) must appear under **vanilla** rules — no wild day delay or spawn-chance gate. Outside monuments, wild guardians use the day ramp + chance in `waterworld.properties`.

### Structure seed-map fidelity (all structures)

Structure biome gates sample vanilla overworld climate at Y≈63. Compare a seed-map tool against:

```
/locate structure minecraft:shipwreck
/locate structure minecraft:ocean_ruin_cold
/locate structure minecraft:ruined_portal_ocean
/locate structure minecraft:buried_treasure
/locate structure minecraft:desert_pyramid
/locate structure minecraft:jungle_temple
/locate structure minecraft:trail_ruins
/locate structure minecraft:swamp_hut
/locate structure minecraft:pillager_outpost
```

Shipwrecks are the ocean (submerged) variant only — beached wrecks and land ruined portals are omitted because they would float on the Y=112 water plane. Ocean ruined portals stay on the seabed. Buried treasure uses vanilla beach biomes (chests still on the ocean floor under those columns).

Seabed under flooded land should be ocean sand/gravel/stone — **not** dripstone-cave spikes (cave biomes only below Y=0).

### Wild guardians

Guardians should spawn naturally in ocean biomes at low frequency (weight 1, compared to squid at ~5), only after the configured day delay.

```
/gamerule spawn_mobs true
```

Swim around in ocean biomes and watch for guardians spawning outside monuments. They should be infrequent. To verify spawn rates, use spectator mode and fly through ocean chunks:

```
/gamemode spectator
```

### Drowned riding guardians

Wild guardians have a 15% chance of spawning with a drowned rider. The rider has a 25% chance of wielding a trident. Look for these while observing wild guardian spawns.

### Drowned on land

Drowned should no longer flee back to water. Find or spawn drowned near a shore and observe:

```
/summon minecraft:drowned ~ ~ ~
```

They should wander on land like regular zombies rather than retreating to water.

### Armored illagers

Illagers (pillagers, vindicators, evokers, witches) can spawn with iron/diamond armor during patrols and raids. Armor chance and tier scale with difficulty. To test:

```
/difficulty hard
/raid ~ ~ ~
```

Inspect spawned raiders for armor pieces.

### Boat dismounting

Illagers, ravagers, wandering traders, and trader llamas will exit boats when on land. To test:

```
/summon minecraft:boat ~ ~ ~
/summon minecraft:pillager ~ ~ ~
```

Push a pillager into a boat (or use the ride command), then move the boat to land. The mob should dismount.

### Boat piloting

Illagers, witches, and wandering traders can steer boats on water. To test:

```
/summon minecraft:boat ~ ~-1 ~
/ride @e[type=minecraft:pillager,limit=1,sort=nearest] mount @e[type=minecraft:boat,limit=1,sort=nearest]
```

The mob should begin steering the boat toward random water destinations.

### Ocean pillager patrols

Pillager patrols spawn in boats on the ocean instead of on land. These follow vanilla patrol timing (after day 5). To fast-forward:

```
/time add 120000
/gamerule spawn_patrols true
```

Wait for a patrol to spawn. They should appear in boats on the water.

### Wandering trader boats

Wandering traders spawn in a boat with one llama passenger instead of on land with two llamas. To force a trader spawn cycle, set the game time forward and wait:

```
/time add 48000
```

The trader should appear in a boat on the water surface.

### Raid spawns in boats

Trigger a raid near the ocean and observe. Raiders should spawn in boats over water, with ravagers in back seats and their jockey riders preserved:

```
/effect give @p minecraft:bad_omen 60 0
```

Then approach a populated area (or place a villager near water).

### Bamboo as sticks in crafting

Bamboo can substitute for sticks in any crafting recipe. To test:

1. Obtain bamboo (Creative inventory or `/give @p minecraft:bamboo 64`)
2. Open a crafting table and try crafting a wooden pickaxe using bamboo instead of sticks:
   - Place planks across the top row, bamboo in the center and bottom-center slots
3. The recipe should work identically to using sticks

Verify with several recipe types:

```
/give @p minecraft:bamboo 64
```

- Tools: pickaxe, axe, sword, shovel, hoe (bamboo in stick slots)
- Torch: bamboo below coal
- Ladder: bamboo in stick slots
- Bow/crossbow: bamboo in stick slots
- Fishing rod: bamboo in stick slots

Disable via config: set `bamboo_replaces_sticks` to `false` in `config/waterworld.properties`.

### Bamboo rafts for mobs

All mob-spawned boats (patrols, raids, wandering traders) now use bamboo rafts instead of oak boats. To verify:

1. Trigger a pillager patrol (see Ocean pillager patrols below) and confirm they spawn in bamboo rafts
2. Trigger a wandering trader spawn and confirm it appears in a bamboo raft
3. Trigger a raid over water and confirm raiders spawn in bamboo rafts

The raft should visually be the bamboo raft model, not a regular wooden boat.

### Boat AI: combat piloting

Illagers (pillagers, vindicators, evokers, witches) now steer toward their attack targets and engage from boats. To test:

```
/summon minecraft:bamboo_raft ~ ~-1 ~
/summon minecraft:pillager ~ ~ ~
/ride @e[type=minecraft:pillager,limit=1,sort=nearest] mount @e[type=minecraft:bamboo_raft,limit=1,sort=nearest]
```

Stand at a distance and observe:
- The pillager should steer the raft toward you
- When within crossbow range (~15 blocks), it should load and fire its crossbow
- The boat should actively move, not sit idle

For melee mobs (vindicators):

```
/summon minecraft:bamboo_raft ~ ~-1 ~
/summon minecraft:vindicator ~ ~ ~
/ride @e[type=minecraft:vindicator,limit=1,sort=nearest] mount @e[type=minecraft:bamboo_raft,limit=1,sort=nearest]
```

The vindicator should steer toward you and attempt melee attacks at close range.

### Boat AI: trader behavior

Wandering traders now flee from threats and approach players while in boats. To test fleeing:

```
/summon minecraft:bamboo_raft ~ ~-1 ~
/summon minecraft:wandering_trader ~ ~ ~
/ride @e[type=minecraft:wandering_trader,limit=1,sort=nearest] mount @e[type=minecraft:bamboo_raft,limit=1,sort=nearest]
/summon minecraft:zombie ~ ~-1 ~5
```

The trader should steer away from the zombie. To test player approach, remove threats and observe the trader steering toward you (stopping ~8 blocks away). When no player or threat is nearby, the trader wanders randomly.

### Buried treasure / treasure maps

Buried treasure uses vanilla **beach** biomes (seed-map XZ) and still places on the **ocean floor** under those columns. Treasure maps from shipwrecks and ocean ruins should point at diggable chests.

```
/locate structure minecraft:buried_treasure
```

Should succeed and lead to a chest on the seabed under a beach column. Optional end-to-end check:

1. Find a shipwreck map chest (`/locate structure minecraft:shipwreck`)
2. Open the map; travel to the X and dig down through sand/gravel on the ocean floor
3. Confirm a buried treasure chest with loot (including possible Heart of the Sea)

### Trail ruins

Trail ruins generate on the seabed (offset under the water plane, buried adaptation). Locate with:

```
/locate structure minecraft:trail_ruins
```

Dive to the marker; expect partially buried trail-ruin pieces on the ocean floor.

### Sea turtles

Turtles should spawn naturally on sand in warm and lukewarm oceans. Egg-laying still requires player-placed sand above water.

```
/gamemode spectator
```

Swim through warm_ocean / lukewarm_ocean chunks with seabed sand and watch for turtles. Config: `turtle_ocean_spawns` (default true), `turtle_spawn_weight` (default 5).

To verify placement on underwater sand (not only beaches):

```
/execute in minecraft:overworld run summon minecraft:turtle ~ ~ ~ {NoAI:1b}
```

Natural spawns should appear on ocean-floor sand without requiring surface beaches.

### Vanilla coast biomes at surface layer

With the same world seed, compare surface-layer biomes (F3 at Y≥108) against a vanilla overworld at the same (x, z):

- Beaches, stony shores, rivers, and mushroom fields should appear where the vanilla seed places them
- Genuine vanilla ocean columns at the surface layer should show climate-matched inland biomes instead

Suggested workflow: note seed and coordinates in Waterworld, create a vanilla world with the same seed, `/tp` to the same x/z, compare F3 biome at Y=120.

### Sea-level water color / fog

At a location where the surface biome is non-ocean (e.g. beach or river mouth in the sky column):

1. Swim at Y=112
2. Observe water tint, fog, and sky color — they should match the **surface** biome, not the underwater ocean biome below

This depends on the 4-block fuzz buffer (Y 108–111 treated as surface layer); see [ARCHITECTURE.md](ARCHITECTURE.md#fuzz-buffer-intentional--do-not-remove).

### Mod Menu client UI

Requires [Mod Menu](https://modrinth.com/mod/modmenu) installed in `run/mods/` (or your client `mods/` folder) **in addition to** this mod. The mod does not bundle Mod Menu.

1. Launch client with both mods
2. Mod list: **Waterworld** shows the mod icon, summary, description, and GitHub releases link
3. Open config from the mod list — `WaterworldConfigScreen` should list categorized `WaterworldConfig` options (activation as Auto/Always/Never cycle) and save changes to `config/waterworld.properties`

Server join still works without the client mod; Mod Menu is optional convenience only.

### Structure substitutes & seabed structures

Witch hut boats and pillager outpost ships replace the vanilla structures at the same IDs (sea surface). Desert pyramids, jungle temples, and trail ruins generate on the **seabed** (flooded) with vanilla seed-map XZ.

```
/locate structure minecraft:swamp_hut
/locate structure minecraft:pillager_outpost
/locate structure minecraft:desert_pyramid
/locate structure minecraft:jungle_pyramid
/locate structure minecraft:trail_ruins
/locate structure minecraft:monument
/locate structure minecraft:shipwreck
/locate structure minecraft:ocean_ruin_warm
```

Still disabled (should fail):

```
/locate structure minecraft:village_plains
/locate structure minecraft:igloo
/locate structure minecraft:mansion
```

## 8. Dedicated server (optional)

1. Fabric server for **26.2** with Loader **0.19.3+**
2. Copy `build/libs/waterworld-26.2-1.0.0.jar` to `mods/`
3. `server.properties`:

   ```properties
   level-type=project-waterworld:waterworld
   ```

## 9. Troubleshooting

| Problem | Fix |
|---------|-----|
| IDE: cannot resolve `net.minecraft` | `genSources vscode` + Clean Java Language Server Workspace |
| `release version 25 not supported` | Let Gradle use Foojay toolchain (`settings.gradle`) |
| **Install fabric** on launch | Remove old jars from `run/mods/` (must match **26.2**); dev client only needs Loom + this mod |
| Hang on **Preparing World for Creation** | Check `run/logs/latest.log` for registry errors (e.g. `preliminary_surface_level` missing in `noise_settings`) |
| **Waterworld** missing in world types | Mod not loaded; check `latest.log` |
| Wrong world shape | Recreate with **Waterworld** world type |
| Version bump | [VERSIONS.md](VERSIONS.md) |
