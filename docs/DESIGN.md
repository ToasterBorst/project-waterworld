# Design — Base gamemode (v1)

## Goal

A **true water world** where standard vanilla progression remains possible: every overworld biome exists above the ocean in the air column, so farms, mob spawning, and (later) structures can behave like normal land when players build into the sky.

This is **not** “flood vanilla terrain.” The surface is a **continuous flat water plane**; terrain exists only as **seabed** below the water.

## World layers

| Layer | Block Y | Blocks | Biomes |
|-------|---------|--------|--------|
| Sky column | **> 112** | Air only | Full **vanilla overworld** `multi_noise` at each (x,z) |
| Surface | **112** | Flat water | Ocean variants (temperature / depth from climate) |
| Water column | Seabed top → 112 | Water | Ocean + correlated climate |
| Seabed | ~**42–76** at highest points | Stone, sand, gravel | Ocean; cave biomes underground |

Constants: [`WaterworldConstants.SEA_LEVEL`](../src/main/java/waterworld/WaterworldConstants.java) = **112**.

## Non-goals (v1)

- Custom structures that break the flat surface
- Gameplay systems (vehicles, resources, progression)
- Modded biomes or blocks
- Nether/End changes (presets pass through vanilla generators)

## Acceptance criteria

See the checklist in [TESTING.md](TESTING.md). Summary:

- Builds on Minecraft **26.1.2**
- Flat water at Y=112, no landmasses or stray water above
- Natural seabed variation in the target depth band
- Vanilla overworld biomes above sea; oceans below
- No mixin-based biome replacement

## Future

- Structure generation on platforms (explicit surface exceptions)
- Spawn tuning for water-surface starts
- Optional client-only UI vs server-only deployment split
