package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

/**
 * Bypasses the monument-only restriction and sky-exposure penalty for
 * naturally spawning guardians in open ocean water.
 * Spawn chance scales with world age (day-based difficulty ramp).
 */
@Mixin(Guardian.class)
public class GuardianSpawnMixin {

	@Inject(method = "checkGuardianSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void waterworld$allowWildGuardianSpawns(
			EntityType<? extends Guardian> type, LevelAccessor level,
			EntitySpawnReason spawnReason, BlockPos pos, RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		if (!WaterworldConfig.INSTANCE.wildGuardianSpawns) return;
		if (spawnReason != EntitySpawnReason.NATURAL) return;

		if (!WaterworldDetection.isActive()) {
			cir.setReturnValue(false);
			return;
		}

		if (level.getFluidState(pos).isSource() && pos.getY() < level.getSeaLevel()) {
			WaterworldConfig config = WaterworldConfig.INSTANCE;

			double scaleFactor = 1.0;
			if (level instanceof ServerLevel serverLevel) {
				scaleFactor = WaterworldConfig.dayScaleFactor(
						serverLevel.getGameTime(), config.guardianMinDays, config.guardianFullStrengthDays);
			}

			if (scaleFactor <= 0.0) {
				cir.setReturnValue(false);
				return;
			}

			double effectiveChance = config.guardianSpawnChance * scaleFactor;
			if (random.nextFloat() < effectiveChance) {
				cir.setReturnValue(true);
			} else {
				cir.setReturnValue(false);
			}
		}
	}
}
