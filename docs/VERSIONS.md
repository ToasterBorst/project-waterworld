# Version matrix

All game and toolchain versions are pinned in [`gradle.properties`](../gradle.properties). The repository and folder name should **not** include a Minecraft version; bump versions here and in `gradle.properties` when updating.

## Current targets (26.1.2 line)

| Component | Property | Value |
|-----------|----------|--------|
| Minecraft | `minecraft_version` | **26.1.2** |
| Fabric Loader | `loader_version` | **0.18.4** |
| Fabric API | `fabric_api_version` | **0.150.0+26.1.2** |
| Fabric Loom | `loom_version` | **1.16-SNAPSHOT** |
| Gradle | wrapper | **9.4.1** |
| Java (compile) | toolchain | **25** |
| Mod | `mod_version` | **1.0.0** |
| Mod ID | `fabric.mod.json` → `id` | **project-waterworld** |

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

[`pack.mcmeta`](../src/main/resources/pack.mcmeta) uses `min_format` / `max_format` `[84, 0]` for Minecraft 26.1.x. Adjust when Mojang changes pack format for a new release (see [datapack.wiki breaking changes](https://datapack.wiki/wiki/info/breaking-changes)).
