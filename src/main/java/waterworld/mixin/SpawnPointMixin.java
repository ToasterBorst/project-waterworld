package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.loader.api.FabricLoader;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.WaterworldMod;
import waterworld.spawn.SpawnIslandGenerator;

@Mixin(MinecraftServer.class)
public class SpawnPointMixin {

	@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
	private static void waterworld$overrideSpawn(ServerLevel level, ServerLevelData levelData,
			boolean spawnBonusChest, boolean isDebug, LevelLoadListener levelLoadListener,
			CallbackInfo ci) {
		if (isDebug) return;
		if (!WaterworldDetection.isWaterworldLevel(level)) return;

		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.spawnIsland) return;

		// With Spawn Party, islands are generated per party origin — not at global world spawn.
		if (FabricLoader.getInstance().isModLoaded("spawnparty")) {
			WaterworldMod.LOGGER.info("Spawn Party present — skipping world-spawn island (per-party islands instead)");
			return;
		}

		ci.cancel();

		levelLoadListener.start(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN, 0);

		ServerChunkCache chunkSource = level.getChunkSource();
		ChunkPos spawnChunk = ChunkPos.containing(chunkSource.randomState().sampler().findSpawnPosition());
		int height = chunkSource.getGenerator().getSpawnHeight(level);
		if (height < level.getMinY()) {
			BlockPos worldPos = spawnChunk.getWorldPosition();
			height = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldPos.getX() + 8, worldPos.getZ() + 8);
		}
		BlockPos spawnPos = spawnChunk.getWorldPosition().offset(8, height, 8);

		levelLoadListener.updateFocus(level.dimension(), ChunkPos.containing(spawnPos));

		int spawnY = SpawnIslandGenerator.generate(level, spawnPos.getX(), spawnPos.getZ());
		spawnPos = new BlockPos(spawnPos.getX(), spawnY, spawnPos.getZ());

		levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), spawnPos, 0.0F, 0.0F));
		WaterworldMod.LOGGER.info("Set world spawn at {} (spawn island)", spawnPos);

		if (spawnBonusChest) {
			level.registryAccess()
					.lookup(Registries.CONFIGURED_FEATURE)
					.flatMap(registry -> registry.get(MiscOverworldFeatures.BONUS_CHEST))
					.ifPresent(feature -> feature.value().place(
							level, chunkSource.getGenerator(), level.getRandom(),
							levelData.getRespawnData().pos()));
		}

		levelLoadListener.finish(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN);
	}
}
