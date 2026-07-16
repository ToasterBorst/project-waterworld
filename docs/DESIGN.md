# Design — Base gamemode (v1)

## Goal

A **true water world** where standard vanilla progression remains possible: every overworld biome exists above the ocean in the air column, so farms, mob spawning, and (later) structures can behave like normal land when players build into the sky.

This is **not** “flood vanilla terrain.” The surface is a **continuous flat water plane**; terrain exists only as **seabed** below the water. Density uses **vanilla** overworld depth/cheese at absolute Y so oceans stay genuinely deep and slopes stay natural; a soft fade (**Y23→83**) trims land tops into seabed hills under the water plane.

## World layers

| Layer | Block Y | Blocks | Biomes |
|-------|---------|--------|--------|
| Sky column | **> 112** | Air only | Vanilla seed biomes kept verbatim (except ocean-tagged → climate-matched inland) |
| Surface fuzz | **108–111** | Water (top quart) | Same as sky column — surface biome drives water color/fog at sea level |
| Surface | **112** | Flat water | Beaches, stony shores, rivers, mushroom fields where the vanilla seed places them; ocean-tagged → inland |
| Water column | Seabed top → 107 | Water | Non-deep oceans where vanilla had land; genuine vanilla oceans (including deep) kept |
| Seabed | up to ~**Y 83** | Stone; warm oceans sand; cold/normal oceans gravel | Ocean biomes (caves only below Y=0) |

Constants: [`WaterworldConstants.SEA_LEVEL`](../src/main/java/waterworld/WaterworldConstants.java) = **112**, fade **23→83** ([`SEA_BED_FADE_START_Y`](../src/main/java/waterworld/WaterworldConstants.java) / [`SEA_BED_MAX_Y`](../src/main/java/waterworld/WaterworldConstants.java)).

## Structures (vanilla IDs, Waterworld placement)

| Structure | Behavior |
|-----------|----------|
| Ocean monuments, shipwrecks, ocean ruined portals, ocean ruins, buried treasure | Vanilla IDs; biome gates use seed-map climate (Y≈63). Land portal biome sites place **ocean-floor** ruined portals (not floating land variants). Beached shipwrecks omitted. |
| Swamp hut / pillager outpost | Boat / ship substitutes at sea surface (vanilla IDs + sets) |
| Desert pyramid / jungle temple | Seabed-settled and flooded; seed-map XZ |
| Trail ruins | Seabed (offset from water surface); seed-map XZ |
| Villages, mansions, igloos, etc. | Disabled (would float / melt) |

## Non-goals (v1)

- Landmasses that break the flat water plane
- Putting land biomes on the seabed (ocean features must keep working)
- Borrowing grass/dirt/terracotta land materials onto the seabed
- Height-remapped / “compressed” topography (causes cleaved pillars)
- Nether/End changes (presets pass through vanilla generators)

## Acceptance criteria

See the checklist in [TESTING.md](TESTING.md). Summary:

- Builds on Minecraft **26.2**
- Flat water at Y=112, no landmasses or stray water above
- Soft-faded seabed with mid-depth relief under former land (~Y23–83), deep floors where vanilla oceans were
- Vanilla seed biomes above sea; oceans below with land columns mapped to non-deep ocean
- Seed-map XZ fidelity for monuments, wrecks, ruins, portals, treasure, pyramids, temples, trail ruins, hut/outpost substitutes
- Buried treasure under beach columns (vanilla biomes), on the ocean floor
