package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidStructure;
import net.minecraft.world.level.levelgen.structure.structures.JungleTempleStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConstants;
import waterworld.WaterworldDetection;

/**
 * After desert pyramids / jungle temples place, fill remaining air below sea
 * level with water so interiors are properly flooded rather than dry pockets.
 */
@Mixin(Structure.class)
public abstract class FloodedScatteredStructureMixin {

	@Inject(method = "afterPlace", at = @At("TAIL"))
	private void waterworld$floodInteriors(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, net.minecraft.util.RandomSource random, BoundingBox chunkBox,
			ChunkPos chunkPos, PiecesContainer pieces, CallbackInfo ci) {
		if (!WaterworldDetection.usesWaterworldBiomeSource(generator)) return;

		Structure self = (Structure) (Object) this;
		if (!(self instanceof DesertPyramidStructure) && !(self instanceof JungleTempleStructure)) {
			return;
		}

		BoundingBox structureBox = pieces.calculateBoundingBox();
		int maxY = Math.min(structureBox.maxY(), WaterworldConstants.seaLevel() - 1);

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = Math.max(structureBox.minX(), chunkBox.minX());
				x <= Math.min(structureBox.maxX(), chunkBox.maxX()); x++) {
			for (int z = Math.max(structureBox.minZ(), chunkBox.minZ());
					z <= Math.min(structureBox.maxZ(), chunkBox.maxZ()); z++) {
				for (int y = structureBox.minY(); y <= maxY; y++) {
					cursor.set(x, y, z);
					if (!chunkBox.isInside(cursor)) continue;
					if (level.getBlockState(cursor).isAir()) {
						level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 2);
					}
				}
			}
		}
	}
}
