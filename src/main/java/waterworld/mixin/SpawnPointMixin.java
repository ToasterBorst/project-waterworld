package waterworld.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConfig;
import waterworld.WaterworldConstants;
import waterworld.spawn.SpawnIslandGenerator;

@Mixin(MinecraftServer.class)
public class SpawnPointMixin {

	@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
	private static void waterworld$overrideSpawn(ServerLevel level, ServerLevelData levelData,
			boolean spawnBonusChest, boolean isDebug, LevelLoadListener levelLoadListener,
			CallbackInfo ci) {
		if (isDebug) return;

		WaterworldConfig config = WaterworldConfig.INSTANCE;
		boolean hasBiomeOverride = config.spawnOceanBiome != null && !config.spawnOceanBiome.isEmpty();
		boolean wantsIsland = config.spawnIsland;

		if (!hasBiomeOverride && !wantsIsland) return;

		ci.cancel();

		levelLoadListener.start(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN, 0);

		BlockPos spawnPos;

		if (hasBiomeOverride) {
			spawnPos = findBiomeSpawn(level, config.spawnOceanBiome);
		} else {
			ServerChunkCache chunkSource = level.getChunkSource();
			ChunkPos spawnChunk = ChunkPos.containing(chunkSource.randomState().sampler().findSpawnPosition());
			int height = chunkSource.getGenerator().getSpawnHeight(level);
			if (height < level.getMinY()) {
				BlockPos worldPos = spawnChunk.getWorldPosition();
				height = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldPos.getX() + 8, worldPos.getZ() + 8);
			}
			spawnPos = spawnChunk.getWorldPosition().offset(8, height, 8);
		}

		levelLoadListener.updateFocus(level.dimension(), ChunkPos.containing(spawnPos));

		if (wantsIsland) {
			int spawnY = SpawnIslandGenerator.generate(level, spawnPos.getX(), spawnPos.getZ());
			spawnPos = new BlockPos(spawnPos.getX(), spawnY, spawnPos.getZ());
		}

		levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), spawnPos, 0.0F, 0.0F));
		ProjectWaterworld.LOGGER.info("Set world spawn at {} (biome override: {}, island: {})",
				spawnPos, hasBiomeOverride, wantsIsland);

		levelLoadListener.finish(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN);
	}

	private static BlockPos findBiomeSpawn(ServerLevel level, String biomeId) {
		Identifier id = biomeId.contains(":")
				? Identifier.parse(biomeId)
				: Identifier.fromNamespaceAndPath("minecraft", biomeId);

		ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, id);

		Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(
				holder -> holder.is(biomeKey),
				BlockPos.ZERO,
				6400, 32, 64
		);

		if (result != null) {
			BlockPos biomePos = result.getFirst();
			int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, biomePos.getX(), biomePos.getZ());
			int spawnY = Math.max(surfaceY, WaterworldConstants.SEA_LEVEL);
			return new BlockPos(biomePos.getX(), spawnY, biomePos.getZ());
		}

		ProjectWaterworld.LOGGER.warn("Could not find biome '{}' near origin, using default spawn", biomeId);
		ServerChunkCache chunkSource = level.getChunkSource();
		ChunkPos fallback = ChunkPos.containing(chunkSource.randomState().sampler().findSpawnPosition());
		int height = chunkSource.getGenerator().getSpawnHeight(level);
		if (height < level.getMinY()) {
			BlockPos worldPos = fallback.getWorldPosition();
			height = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldPos.getX() + 8, worldPos.getZ() + 8);
		}
		return fallback.getWorldPosition().offset(8, height, 8);
	}
}
