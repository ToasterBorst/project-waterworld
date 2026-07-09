# Design — Base gamemode (v1)

## Goal

A **true water world** where standard vanilla progression remains possible: every overworld biome exists above the ocean in the air column, so farms, mob spawning, and (later) structures can behave like normal land when players build into the sky.

This is **not** “flood vanilla terrain.” The surface is a **continuous flat water plane**; terrain exists only as **seabed** below the water.

## World layers

| Layer | Block Y | Blocks | Biomes |
|-------|---------|--------|--------|
| Sky column | **> 112** | Air only | Vanilla seed biomes kept verbatim (except ocean-tagged → climate-matched inland) |
| Surface fuzz | **108–111** | Water (top quart) | Same as sky column — surface biome drives water color/fog at sea level |
| Surface | **112** | Flat water | Beaches, stony shores, rivers, mushroom fields where the vanilla seed places them; ocean-tagged → inland |
| Water column | Seabed top → 107 | Water | Non-deep oceans where vanilla had land; genuine vanilla oceans (including deep) kept |
| Seabed | ~**42–76** at highest points | Stone, sand, gravel | Ocean + cave biomes underground |

Constants: [`WaterworldConstants.SEA_LEVEL`](../src/main/java/waterworld/WaterworldConstants.java) = **112**.

## Non-goals (v1)

- Custom structures that break the flat surface
- Gameplay systems (vehicles, resources, progression)
- Biome-source replacement mods (TerraBlender-style); parameter-space injection is supported
- Nether/End changes (presets pass through vanilla generators)

## Acceptance criteria

See the checklist in [TESTING.md](TESTING.md). Summary:

- Builds on Minecraft **26.2**
- Flat water at Y=112, no landmasses or stray water above
- Natural seabed variation in the target depth band
- Vanilla seed biomes above sea (including coasts and rivers where valid); oceans below with land columns mapped to non-deep ocean
- Buried treasure on ocean floor; trail ruins disabled
- No mixin-based biome replacement (biome logic lives in `WaterworldBiomeSource`)

## Future

- Structure generation on platforms (explicit surface exceptions)
- Spawn tuning for water-surface starts
