# Project Waterworld

Fabric mod for Minecraft **26.1.2** that generates a true water world: a flat ocean surface at **Y=112**, natural seabed below, and vanilla overworld biome distribution in the air column above the water so players can build platforms and get normal farming, mob, and feature behavior.

## Requirements

- **JDK 25** (Gradle can provision via Foojay; see [`settings.gradle`](settings.gradle))
- **Git**

## Quick start

```powershell
git clone https://github.com/ToasterBorst/project-waterworld.git
cd project-waterworld
.\gradlew.bat build
.\run-client.bat
```

In **Create New World**, choose world type **Waterworld**.

Versions are pinned in [`gradle.properties`](gradle.properties). Dev runs use Fabric Loom to download Minecraft, Loader, and API—no separate Fabric install needed for local testing.

## Documentation

| Doc | Purpose |
|-----|---------|
| [docs/VERSIONS.md](docs/VERSIONS.md) | Pinned MC / Fabric / Java versions |
| [docs/TESTING.md](docs/TESTING.md) | Build, run, and in-game verification |
| [docs/DESIGN.md](docs/DESIGN.md) | Goals and acceptance criteria |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Datapack vs mod split |

## Build output

`build/libs/project-waterworld-1.0.0.jar`

## License

CC0-1.0 (see [LICENSE](LICENSE))
