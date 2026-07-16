package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import waterworld.WaterworldDetection;

/**
 * Desert pyramids and jungle temples settle with {@code MOTION_BLOCKING_NO_LEAVES},
 * which treats water as ground and would park them on the Y=112 water plane.
 * In Waterworld, settle to the solid ocean floor instead.
 */
@Mixin(ScatteredFeaturePiece.class)
public abstract class ScatteredFeatureSeabedMixin {

	@Redirect(
			method = {
					"updateHeightPositionToLowestGroundHeight",
					"updateAverageGroundHeight"
			},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/LevelAccessor;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"
			)
	)
	private BlockPos waterworld$oceanFloorHeightmapPos(LevelAccessor level, Heightmap.Types type, BlockPos pos) {
		if (WaterworldDetection.usesWaterworldBiomeSource(level)) {
			return level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, pos);
		}
		return level.getHeightmapPos(type, pos);
	}
}
