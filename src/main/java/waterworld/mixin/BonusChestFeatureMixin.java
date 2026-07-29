package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.BonusChestFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import waterworld.WaterworldDetection;

/**
 * Vanilla bonus chests settle with {@code MOTION_BLOCKING_NO_LEAVES}, which
 * treats water as ground and parks them on the ocean surface. In Waterworld,
 * settle to the solid ocean floor instead.
 */
@Mixin(BonusChestFeature.class)
public class BonusChestFeatureMixin {

	@Redirect(
			method = "place",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/WorldGenLevel;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"
			)
	)
	private BlockPos waterworld$oceanFloorBonusChest(WorldGenLevel level, Heightmap.Types type, BlockPos pos) {
		if (WaterworldDetection.usesWaterworldBiomeSource(level)) {
			return level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, pos);
		}
		return level.getHeightmapPos(type, pos);
	}
}
