# Design — Base gamemode (v1)

## Goal

A **true water world** where standard vanilla progression remains possible: every overworld biome exists above the ocean in the air column, so farms, mob spawning, and (later) structures can behave like normal land when players build into the sky.

This is **not** “flood vanilla terrain.” The surface is a **continuous flat water plane**; terrain exists only as **seabed** below the water. Density uses **vanilla** overworld depth/cheese so oceans stay genuinely deep; a soft fade (≈Y48→76) trims land tops into mid-depth seabed hills under the water plane (less mesa than a hard low ceiling).

## World layers

| Layer | Block Y | Blocks | Biomes |
|-------|---------|--------|--------|
| Sky column | **> 112** | Air only | Vanilla seed biomes kept verbatim (except ocean-tagged → climate-matched inland) |
| Surface fuzz | **108–111** | Water (top quart) | Same as sky column — surface biome drives water color/fog at sea level |
| Surface | **112** | Flat water | Beaches, stony shores, rivers, mushroom fields where the vanilla seed places them; ocean-tagged → inland |
| Water column | Seabed top → 107 | Water | Non-deep oceans where vanilla had land; genuine vanilla oceans (including deep) kept |
| Seabed | up to ~**Y 76** | Stone, sand, gravel, climate accents (sandstone/dirt) | Ocean biomes (caves only below Y=0) |

Constants: [`WaterworldConstants.SEA_LEVEL`](../src/main/java/waterworld/WaterworldConstants.java) = **112**, [`SEA_BED_MAX_Y`](../src/main/java/waterworld/WaterworldConstants.java) = **76**.

## Structures (vanilla IDs, Waterworld placement)

| Structure | Behavior |
|-----------|----------|
| Ocean monuments, shipwrecks, ocean ruined portals, ocean ruins, buried treasure | Vanilla IDs; biome gates use seed-map climate (Y≈63). Land ruined-portal / beached-shipwreck variants omitted (would float on the water plane) |
| Swamp hut / pillager outpost | Boat / ship substitutes at sea surface (vanilla IDs + sets) |
| Desert pyramid / jungle temple | Seabed-settled and flooded; seed-map XZ |
| Trail ruins | Seabed (offset from water surface); seed-map XZ |
| Villages, mansions, igloos, etc. | Disabled (would float) |

## Non-goals (v1)

- Landmasses that break the flat water plane
- Putting land biomes on the seabed (ocean features must keep working)
- Nether/End changes (presets pass through vanilla generators)

## Acceptance criteria

See the checklist in [TESTING.md](TESTING.md). Summary:

- Builds on Minecraft **26.2**
- Flat water at Y=112, no landmasses or stray water above
- Soft-faded seabed with mid-depth relief under former land (~Y48–76), deep floors where vanilla oceans were
- Vanilla seed biomes above sea; oceans below with land columns mapped to non-deep ocean
- Seed-map XZ fidelity for monuments, wrecks, ruins, portals, treasure, pyramids, temples, trail ruins, hut/outpost substitutes
- Buried treasure under beach columns (vanilla biomes), on the ocean floor
