# Testing Project Waterworld

Step-by-step guide to build, run, and verify the mod locally.

## 1. One-time setup

1. Install **Java 25** for the IDE (Gradle can auto-provision JDK 25 for builds via Foojay).
2. Optional: IntelliJ **2025.3+** or Cursor/VS Code with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack).

## 2. Build the mod

From the repository root in PowerShell (prefix scripts with `.\`):

```powershell
.\gradlew.bat build
```

Success produces: `build\libs\project-waterworld-1.0.0.jar`

If dependencies fail:

```powershell
.\gradlew.bat build --refresh-dependencies
```

## 3. Run in development (fastest loop)

```powershell
.\run-client.bat
```

Same as `.\gradlew.bat runClient`. First launch downloads Minecraft **26.1.2** and Fabric (several minutes). Versions come from [`gradle.properties`](../gradle.properties)—no separate Fabric installer needed for dev.

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
| Seabed depth | Underwater: highest seabed roughly **Y=42–76** |
| Biomes above water | F3 in air: vanilla overworld biomes by region |
| Biomes at/below surface | F3 at Y≤112: oceans; caves deeper down |
| Datapack active | `/datapack list` — `project-waterworld` enabled |
| Mod loaded | `logs/latest.log` — `Initializing Project Waterworld` |

## 7. Dedicated server (optional)

1. Fabric server for **26.1.2** with Loader **0.18.4+**
2. Copy `build/libs/project-waterworld-1.0.0.jar` to `mods/`
3. `server.properties`:

   ```properties
   level-type=project-waterworld:waterworld
   ```

## 8. Troubleshooting

| Problem | Fix |
|---------|-----|
| IDE: cannot resolve `net.minecraft` | `genSources vscode` + Clean Java Language Server Workspace |
| `release version 25 not supported` | Let Gradle use Foojay toolchain (`settings.gradle`) |
| **Install fabric** on launch | Remove old jars from `run/mods/` (must match **26.1.2**); dev client only needs Loom + this mod |
| Hang on **Preparing World for Creation** | Check `run/logs/latest.log` for registry errors (e.g. `preliminary_surface_level` missing in `noise_settings`) |
| **Waterworld** missing in world types | Mod not loaded; check `latest.log` |
| Wrong world shape | Recreate with **Waterworld** world type |
| Version bump | [VERSIONS.md](VERSIONS.md) |
