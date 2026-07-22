package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.structure.SpawnZonePiece;
import waterworld.worldgen.WaterworldBiomeTags;

import java.util.List;

/**
 * Turtle empty-block exception, seabed Y snap for turtle biomes, plus spawn-zone Y
 * clamping for ocean hut/ship structure overrides (vanilla Y lottery uses
 * {@code WORLD_SURFACE+1}, which does not cover structure decks above the waterline).
 */
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

	@Inject(method = "isValidEmptySpawnBlock", at = @At("HEAD"), cancellable = true)
	private static void waterworld$allowWaterForTurtles(
			BlockGetter level, BlockPos pos, BlockState state, FluidState fluid, EntityType<?> type,
			CallbackInfoReturnable<Boolean> cir) {
		if (!WaterworldDetection.isActive()) return;
		if (!WaterworldConfig.INSTANCE.turtleOceanSpawns) return;
		if (type != EntityTypes.TURTLE) return;
		if (!fluid.is(FluidTags.WATER)) return;
		if (state.isCollisionShapeFullBlock(level, pos)) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(true);
	}

	@Inject(method = "getRandomPosWithin", at = @At("RETURN"), cancellable = true)
	private static void waterworld$snapSpawnY(
			Level level, LevelChunk chunk, CallbackInfoReturnable<BlockPos> cir) {
		if (!WaterworldDetection.isActive()) return;
		if (!(level instanceof ServerLevel serverLevel)) return;

		BlockPos vanilla = cir.getReturnValue();
		if (vanilla == null) return;

		int x = vanilla.getX();
		int z = vanilla.getZ();
		int y = vanilla.getY();

		Integer zoneY = waterworld$clampYToSpawnZone(serverLevel, chunk.getPos(), x, z, y);
		if (zoneY != null) {
			y = zoneY;
		} else if (WaterworldConfig.INSTANCE.turtleOceanSpawns) {
			Integer turtleY = waterworld$findTurtleSeabedY(serverLevel, x, z, y);
			if (turtleY != null) {
				y = turtleY;
			}
		}

		if (y != vanilla.getY()) {
			cir.setReturnValue(new BlockPos(x, y, z));
		}
	}

	/**
	 * When {@code (x,z)} lies in a {@link SpawnZonePiece} spawn zone with monster
	 * overrides, re-roll Y uniformly inside the zone's vertical band (vanilla lottery
	 * character, structure-relative range). Returns null to keep vanilla Y.
	 */
	@Unique
	@Nullable
	private static Integer waterworld$clampYToSpawnZone(
			ServerLevel level, ChunkPos chunkPos, int x, int z, int vanillaY) {
		StructureManager structures = level.structureManager();
		List<StructureStart> starts = structures.startsForStructure(chunkPos, structure -> {
			StructureSpawnOverride override = structure.spawnOverrides().get(MobCategory.MONSTER);
			return override != null && !override.spawns().unwrap().isEmpty();
		});
		if (starts.isEmpty()) return null;

		for (StructureStart start : starts) {
			if (!start.isValid()) continue;
			Structure structure = start.getStructure();
			StructureSpawnOverride override = structure.spawnOverrides().get(MobCategory.MONSTER);
			if (override == null) continue;

			BoundingBox zone = waterworld$findSpawnZoneAt(start, override, x, z);
			if (zone == null) continue;

			// Preserve lottery: pick a random Y inside the structure spawn band.
			int minY = zone.minY();
			int maxY = zone.maxY();
			if (maxY < minY) continue;
			if (vanillaY >= minY && vanillaY <= maxY) {
				return vanillaY;
			}
			return Mth.randomBetweenInclusive(level.getRandom(), minY, maxY);
		}
		return null;
	}

	@Unique
	@Nullable
	private static BoundingBox waterworld$findSpawnZoneAt(
			StructureStart start, StructureSpawnOverride override, int x, int z) {
		if (override.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE) {
			for (StructurePiece piece : start.getPieces()) {
				if (piece instanceof SpawnZonePiece spawnZonePiece) {
					BoundingBox zone = spawnZonePiece.getSpawnZone();
					if (x >= zone.minX() && x <= zone.maxX() && z >= zone.minZ() && z <= zone.maxZ()) {
						return zone;
					}
				}
			}
			return null;
		}

		// STRUCTURE / "full" — prefer SpawnZonePiece zone when present.
		for (StructurePiece piece : start.getPieces()) {
			if (piece instanceof SpawnZonePiece spawnZonePiece) {
				BoundingBox zone = spawnZonePiece.getSpawnZone();
				if (x >= zone.minX() && x <= zone.maxX() && z >= zone.minZ() && z <= zone.maxZ()) {
					return zone;
				}
			}
		}

		BoundingBox full = start.getBoundingBox();
		if (x >= full.minX() && x <= full.maxX() && z >= full.minZ() && z <= full.maxZ()) {
			return full;
		}
		return null;
	}

	/**
	 * Snap to the top sand block under water in turtle biomes so spawn rules
	 * can succeed (vanilla Y lottery almost never hits the thin seabed).
	 */
	@Unique
	@Nullable
	private static Integer waterworld$findTurtleSeabedY(ServerLevel level, int x, int z, int startY) {
		Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
		if (!biome.is(WaterworldBiomeTags.TURTLE_SPAWNS)) return null;

		int maxY = Math.min(startY, level.getSeaLevel() + 3);
		int minY = Math.max(level.getMinY() + 1, 32);
		for (int y = maxY; y >= minY; y--) {
			BlockPos feet = new BlockPos(x, y, z);
			BlockPos below = feet.below();
			if (!TurtleEggBlock.onSand(level, below)) continue;
			if (!level.getFluidState(feet).is(FluidTags.WATER)) continue;

			BlockState feetState = level.getBlockState(feet);
			FluidState feetFluid = feetState.getFluidState();
			if (!NaturalSpawner.isValidEmptySpawnBlock(level, feet, feetState, feetFluid, EntityTypes.TURTLE)) {
				continue;
			}

			BlockPos head = feet.above();
			BlockState headState = level.getBlockState(head);
			if (!NaturalSpawner.isValidEmptySpawnBlock(
					level, head, headState, headState.getFluidState(), EntityTypes.TURTLE)) {
				continue;
			}

			return y;
		}
		return null;
	}
}
