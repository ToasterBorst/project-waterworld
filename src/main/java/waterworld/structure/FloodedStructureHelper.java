package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidStructure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.structures.JungleTempleStructure;
import net.minecraft.world.level.material.Fluids;
import waterworld.WaterworldConstants;

/**
 * Fills structure interiors below sea level with water (and waterlogs
 * waterloggable blocks) so seabed temples/ruins are fully flooded.
 */
public final class FloodedStructureHelper {
	private FloodedStructureHelper() {
	}

	public static boolean shouldFlood(Structure structure) {
		if (structure instanceof DesertPyramidStructure || structure instanceof JungleTempleStructure) {
			return true;
		}
		// Trail ruins are a buried underground jigsaw (no dedicated StructureType).
		return structure instanceof JigsawStructure
			&& structure.terrainAdaptation() == TerrainAdjustment.BURY
			&& structure.step() == GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
	}

	public static void floodBelowSeaLevel(WorldGenLevel level, BoundingBox structureBox, BoundingBox chunkBox) {
		int maxY = Math.min(structureBox.maxY(), WaterworldConstants.seaLevel() - 1);
		if (maxY < structureBox.minY()) {
			return;
		}

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		var water = Fluids.WATER.defaultFluidState();

		for (int x = Math.max(structureBox.minX(), chunkBox.minX());
				x <= Math.min(structureBox.maxX(), chunkBox.maxX()); x++) {
			for (int z = Math.max(structureBox.minZ(), chunkBox.minZ());
					z <= Math.min(structureBox.maxZ(), chunkBox.maxZ()); z++) {
				for (int y = structureBox.minY(); y <= maxY; y++) {
					cursor.set(x, y, z);
					if (!chunkBox.isInside(cursor)) {
						continue;
					}

					BlockState state = level.getBlockState(cursor);
					if (state.isAir() || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR)) {
						level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 2);
						continue;
					}

					if (state.getBlock() instanceof LiquidBlockContainer container
							&& container.canPlaceLiquid(null, level, cursor, state, Fluids.WATER)) {
						container.placeLiquid(level, cursor, state, water);
					}
				}
			}
		}
	}
}
