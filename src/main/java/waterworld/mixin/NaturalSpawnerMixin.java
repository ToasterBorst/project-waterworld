package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

import java.util.List;

/**
 * Turtle empty-block exception, plus always-on floor Y snap for monster
 * {@code spawn_overrides} pieces so ocean huts/ships can use vanilla NaturalSpawner
 * (uniform Y lottery otherwise almost never hits thin decks).
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
	private static void waterworld$snapYForMonsterOverridePieces(
			Level level, LevelChunk chunk, CallbackInfoReturnable<BlockPos> cir) {
		if (!WaterworldDetection.isActive()) return;
		if (!(level instanceof ServerLevel serverLevel)) return;

		BlockPos vanilla = cir.getReturnValue();
		if (vanilla == null) return;

		int x = vanilla.getX();
		int z = vanilla.getZ();
		Integer floorY = waterworld$findOverrideFloorY(serverLevel, chunk.getPos(), x, z);
		if (floorY != null && floorY != vanilla.getY()) {
			cir.setReturnValue(new BlockPos(x, floorY, z));
		}
	}

	/**
	 * If {@code (x,z)} lies in a structure piece with a non-empty monster
	 * spawn_override, return the topmost air-over-sturdy feet Y in that column
	 * inside the piece BB; otherwise null (keep vanilla Y).
	 */
	@Unique
	private static Integer waterworld$findOverrideFloorY(ServerLevel level, ChunkPos chunkPos, int x, int z) {
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

			if (override.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE) {
				for (StructurePiece piece : start.getPieces()) {
					Integer y = waterworld$scanFloorY(level, x, z, piece.getBoundingBox());
					if (y != null) return y;
				}
			} else {
				Integer y = waterworld$scanFloorY(level, x, z, start.getBoundingBox());
				if (y != null) return y;
			}
		}
		return null;
	}

	@Unique
	private static Integer waterworld$scanFloorY(Level level, int x, int z, BoundingBox bb) {
		if (x < bb.minX() || x > bb.maxX() || z < bb.minZ() || z > bb.maxZ()) {
			return null;
		}

		for (int y = bb.maxY(); y >= bb.minY() + 1; y--) {
			BlockPos feet = new BlockPos(x, y, z);
			BlockPos below = feet.below();
			BlockState belowState = level.getBlockState(below);
			if (!belowState.isFaceSturdy(level, below, Direction.UP)) continue;
			if (!belowState.getFluidState().isEmpty()) continue;

			BlockState feetState = level.getBlockState(feet);
			FluidState feetFluid = feetState.getFluidState();
			if (!NaturalSpawner.isValidEmptySpawnBlock(level, feet, feetState, feetFluid, EntityTypes.WITCH)) {
				continue;
			}

			BlockPos head = feet.above();
			BlockState headState = level.getBlockState(head);
			if (!NaturalSpawner.isValidEmptySpawnBlock(
					level, head, headState, headState.getFluidState(), EntityTypes.WITCH)) {
				continue;
			}

			return y;
		}
		return null;
	}
}
