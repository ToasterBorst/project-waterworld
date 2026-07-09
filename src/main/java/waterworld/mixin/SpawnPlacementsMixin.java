package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;

/**
 * Lets turtle spawn attempts pass ON_GROUND placement checks on underwater sand.
 */
@Mixin(SpawnPlacements.class)
public class SpawnPlacementsMixin {

	@Inject(method = "isSpawnPositionOk", at = @At("HEAD"), cancellable = true)
	private static void waterworld$allowUnderwaterTurtlePlacement(
			EntityType<?> type, LevelReader level, BlockPos pos,
			CallbackInfoReturnable<Boolean> cir) {
		if (!WaterworldConfig.INSTANCE.turtleOceanSpawns) return;
		if (type != EntityTypes.TURTLE) return;

		FluidState fluid = level.getFluidState(pos);
		if (!fluid.is(FluidTags.WATER)) return;
		if (!TurtleEggBlock.onSand(level, pos)) return;

		cir.setReturnValue(true);
	}
}
