package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

/**
 * Accepts water-filled spawn blocks for turtles so natural spawning can reach rule checks.
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
}
