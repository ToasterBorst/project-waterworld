# Architecture

## Overview

```mermaid
flowchart TB
    subgraph mod [Fabric mod JAR]
        PW[ProjectWaterworld]
        WBS[WaterworldBiomeSource]
        MM[WaterworldModMenu optional client]
        PW --> WBS
        PW --> DP[Builtin datapack registration]
        PW --> MM
    end
    subgraph data [Embedded datapack]
        PRESET[world_preset/waterworld.json]
        NOISE[noise_settings/waterworld.json]
        STRUCT[structure overrides]
        PRESET --> NOISE
    end
    DP --> data
    PRESET --> WBS
    NOISE --> GEN[Vanilla noise chunk generator]
    WBS --> GEN
```

## Responsibilities

| Component | Owns |
|-----------|------|
| **Datapack** (`data/project-waterworld/`) | World preset, noise settings (sea level, density caps, surface rules), UI tag for normal presets |
| **Datapack overrides** (`data/minecraft/`) | Buried-treasure ocean gate; structure sets (emptied villages/mansions/…; restored pyramids/temples/trail ruins; hut/outpost substitutes); trail ruins height offset |
| **Mod Java** | `WaterworldBiomeSource`, structure types/pieces, seabed settle + flood mixins, spawn/boat mixins, optional Mod Menu |
| **Mod Java (client, optional)** | `WaterworldModMenu` + `WaterworldConfigScreen` — in-game config when Mod Menu is installed (`compileOnly` dependency; no runtime requirement) |
| **Vanilla** | Chunk noise, aquifers, ore, carving (tuned via JSON) |

## Biome selection

[`WaterworldBiomeSource`](../src/main/java/waterworld/worldgen/WaterworldBiomeSource.java) wraps a single vanilla `MultiNoiseBiomeSource` (overworld preset). At each noise position it **samples vanilla first** and **substitutes only when the layer demands it** — same (x, z) climate noise as vanilla; vertical layer and tag classification decide whether the sample is kept or remapped.

### Layers

| Layer | Block Y | Rule |
|-------|---------|------|
| **Surface** | ≥ sea level − 4 (fuzz buffer) | Keep land, coast, river, and mushroom-field biomes verbatim. Replace only **ocean-tagged** biomes with a climate-matched inland biome (continentalness raised to −0.10; other climate params preserved). |
| **Underwater** | &lt; sea level − 4 | Keep **ocean** biomes verbatim. Remap vanilla land **and** mid-depth cave/underground biomes to a climate-matched **non-deep** ocean (continentalness [−0.45, −0.20]) so the seabed stays ocean-featured (no dripstone-cave floors under continents). Genuine vanilla oceans keep their deep variants. Cave biomes are kept only below **Y=0**. |

Sea level is **112** ([`WaterworldConstants.SEA_LEVEL`](../src/main/java/waterworld/WaterworldConstants.java)).

**Structure biome checks:** All structure placement biome gates sample the **vanilla overworld** climate at Y≈63 (no Waterworld remapping) — both `Structure.isValidBiome` and monument `getBiomesWithin`. That keeps XZ on vanilla seed maps while surface/underwater remapping still drives rendering and mobs.

### Fuzz buffer (intentional — do not remove)

Biome noise is fuzzed by up to one quart (4 blocks). The constant `BIOME_FUZZ_BUFFER = 4` treats block Y **108–111** (the top water quart) as part of the **surface layer**, not the underwater layer. Everything at and just below sea level therefore resolves to the **surface biome** for rendering.

**Why:** Water tint, fog, and sky at Y=112 while swimming or boating match the surface biome (beach, river mouth, etc.) instead of the underwater ocean biome. Removing or shrinking this buffer would “fix” a visual mismatch that is deliberate.

### Tag-driven classification

Ocean: `minecraft:is_ocean`, `c:is_ocean`, `c:is_deep_ocean`. Cave/underground: `c:is_cave`, `c:is_underground`. Future vanilla and modded biomes with correct tags are handled without code changes.

### Modded biome compatibility

| Approach | Compatibility |
|----------|----------------|
| Mods that **inject into the vanilla multi-noise parameter space** (add biomes to the overworld preset with standard tags) | **Works** — picked up by the wrapped overworld source and tag rules |
| Mods that **replace the biome source** entirely (e.g. TerraBlender-style custom sources) | **Out of scope** — they bypass this world preset |

### Opting a modded ocean biome into Waterworld edges

Air-column / underwater remapping already follows `minecraft:is_ocean` / `c:is_*` tags. For edge features, add your biome id to the matching datapack tag under `data/project-waterworld/tags/worldgen/biome/` (or a datapack that appends to it):

| Tag | Effect |
|-----|--------|
| `turtle_spawns` | Ocean turtle natural spawns + underwater sand spawn rules |
| `seabed_sand` | Sand seabed surface rule |
| `seabed_gravel` | Gravel seabed surface rule |
| `spawn_island_cold` | Cold/frozen spawn-island materials |
| `spawn_island_warm` | Warm spawn-island coral decoration |

## Terrain

[`noise_settings/waterworld.json`](../src/main/resources/data/project-waterworld/worldgen/noise_settings/waterworld.json):

- `sea_level`: **112**
- Vanilla `overworld/depth` + `overworld/sloped_cheese` (preserves deep oceans)
- Soft density fade **Y48→76** trims land tops into seabed hills under the water plane
- `preliminary_surface` upper clamp ~80; climate surface accents (sand/sandstone/dirt) plus ocean sand/gravel tags

## Versioning

See [VERSIONS.md](VERSIONS.md) for the full matrix and upgrade procedure. Summary: Minecraft **26.2**, Mojang mappings (no Yarn), Loom **1.17**, Gradle **9.5.1**, Java **25**.
