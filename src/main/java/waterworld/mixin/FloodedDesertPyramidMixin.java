package waterworld.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldDetection;
import waterworld.structure.FloodedStructureHelper;

/**
 * {@link DesertPyramidStructure#afterPlace} overrides the base method without
 * calling {@code super}, so {@link FloodedScatteredStructureMixin} never runs
 * for pyramids. Flood after suspicious-sand placement instead.
 */
@Mixin(DesertPyramidStructure.class)
public abstract class FloodedDesertPyramidMixin {

	@Inject(method = "afterPlace", at = @At("TAIL"))
	private void waterworld$floodPyramid(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, net.minecraft.util.RandomSource random, BoundingBox chunkBox,
			ChunkPos chunkPos, PiecesContainer pieces, CallbackInfo ci) {
		if (!WaterworldDetection.usesWaterworldBiomeSource(generator)) {
			return;
		}

		FloodedStructureHelper.floodBelowSeaLevel(level, pieces.calculateBoundingBox(), chunkBox);
	}
}
