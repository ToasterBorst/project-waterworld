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
import waterworld.spawn.OceanMonumentChecks;

/**
 * Allows rare wild guardians in open ocean, with a day-based ramp.
 * Monument guardians must keep vanilla rules — never apply the wild delay/chance
 * inside an ocean monument.
 */
@Mixin(Guardian.class)
public class GuardianSpawnMixin {

	@Inject(method = "checkGuardianSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void waterworld$allowWildGuardianSpawns(
			EntityType<? extends Guardian> type, LevelAccessor level,
			EntitySpawnReason spawnReason, BlockPos pos, RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		if (!WaterworldDetection.isActive()) return;

		// Structure / non-natural spawns (elders, structure overrides): vanilla only.
		if (spawnReason != EntitySpawnReason.NATURAL) return;

		// Inside a monument: do not override — vanilla monument spawn rules apply.
		if (level instanceof ServerLevel serverLevel && OceanMonumentChecks.isInOceanMonument(serverLevel, pos)) {
			return;
		}

		if (!WaterworldConfig.INSTANCE.wildGuardianSpawns) return;

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
