# Version matrix

All game and toolchain versions are pinned in [`gradle.properties`](../gradle.properties). The repository and folder name should **not** include a Minecraft version; bump versions here and in `gradle.properties` when updating.

## Current targets (26.2 — Chaos Cubed)

| Component | Property | Value |
|-----------|----------|--------|
| Minecraft | `minecraft_version` | **26.2** |
| Fabric Loader | `loader_version` | **0.19.3** |
| Fabric API | `fabric_api_version` | **0.153.0+26.2** |
| Fabric Loom | `loom_version` | **1.17** |
| Gradle | wrapper | **9.5.1** |
| Java (compile) | toolchain | **25** |
| Mod | `mod_version` | **1.0.0** |
| Mod ID | `fabric.mod.json` → `id` | **project-waterworld** |

## Optional compile-only dependencies

These are declared in `build.gradle` for compile-time APIs only. They are **not** bundled and **not** required at runtime.

| Dependency | Version | Scope | Runtime |
|------------|---------|-------|---------|
| [Mod Menu](https://modrinth.com/mod/modmenu) (`com.terraformersmc:modmenu`) | **20.0.0-beta.4** | `compileOnly` | Optional — install client-side for mod-list icon, metadata, and in-game config screen |

## How to update

1. Check [fabricmc.net/develop](https://fabricmc.net/develop) for current Loader, Loom, and API strings.
2. Edit `gradle.properties` (Minecraft + `fabric_api_version` together).
3. Run:

   ```powershell
   .\gradlew.bat build --refresh-dependencies
   .\gradlew.bat genSources vscode
   ```

4. Fix compile errors using [Fabric porting docs](https://docs.fabricmc.net/develop/porting/).
5. Update this table and [`fabric.mod.json`](../src/main/resources/fabric.mod.json) `depends.minecraft` if the supported range changes.

## Datapack format

[`pack.mcmeta`](../src/main/resources/pack.mcmeta) uses `min_format` `[107, 1]` for Minecraft 26.2 and `max_format` `[200, 0]` to avoid future incompatibility warnings. Adjust `min_format` when Mojang changes pack format for a new release (see [datapack.wiki breaking changes](https://datapack.wiki/wiki/info/breaking-changes)).

## Biome source architecture

`WaterworldBiomeSource` wraps one overworld `MultiNoiseBiomeSource`, samples vanilla first, and substitutes only when the surface or underwater layer demands it (tag-driven). Mods that inject biomes into the vanilla parameter space work; mods that replace the biome source entirely are out of scope. See [`WaterworldBiomeSource.java`](../src/main/java/waterworld/worldgen/WaterworldBiomeSource.java) and [ARCHITECTURE.md](ARCHITECTURE.md#biome-selection).
