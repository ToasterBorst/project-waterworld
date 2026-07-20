package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldDetection;
import waterworld.spawn.WaterworldMobTypes;

/**
 * Keeps water-avoiding illagers from diving off structures while allowing
 * normal deck chase on full planks.
 *
 * <p>STRAFE: stop instead of vanilla's walk-forward fallback.
 *
 * <p>MOVE_TO / JUMPING: block only open-water drops and partial hull-lip
 * footing (stairs/shelves beside ocean). Stand-and-shoot when the target is
 * swimming is handled by {@link RangedCrossbowAttackGoalMixin}.
 */
@Mixin(MoveControl.class)
public abstract class MoveControlMixin {
	private static final double[] PROBE_DISTANCES = {1.0, 1.5, 2.0};
	private static final int HAZARD_DEPTH = 4;

	@Shadow
	@Final
	protected Mob mob;

	@Shadow
	protected double wantedX;

	@Shadow
	protected double wantedZ;

	@Shadow
	protected float strafeForwards;

	@Shadow
	protected float strafeRight;

	/** True after {@code setWantedPosition}; cleared by {@code strafe}. */
	@Unique
	private boolean waterworld$moveToActive;

	@Invoker("isWalkable")
	abstract boolean waterworld$isWalkable(float dx, float dz);

	@Inject(method = "setWantedPosition", at = @At("TAIL"))
	private void waterworld$markMoveTo(double x, double y, double z, double speedModifier, CallbackInfo ci) {
		this.waterworld$moveToActive = true;
	}

	@Inject(method = "strafe", at = @At("HEAD"))
	private void waterworld$markStrafe(float forwards, float right, CallbackInfo ci) {
		this.waterworld$moveToActive = false;
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/ai/control/MoveControl;isWalkable(FF)Z"
			)
	)
	private boolean waterworld$stopInsteadOfForwardFallback(MoveControl self, float dx, float dz) {
		boolean walkable = ((MoveControlMixin) (Object) self).waterworld$isWalkable(dx, dz);
		if (walkable) return true;

		if (WaterworldDetection.isActive() && WaterworldMobTypes.isHostileBoatPilot(this.mob)) {
			this.strafeForwards = 0.0F;
			this.strafeRight = 0.0F;
			return true;
		}
		return false;
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Mob;setSpeed(F)V"
			)
	)
	private void waterworld$gateSpeedOntoWater(Mob mob, float speed) {
		if (!this.waterworld$moveToActive) {
			mob.setSpeed(speed);
			return;
		}

		if (waterworld$shouldBlockTowardWanted(mob)) {
			waterworld$stopHorizontal(mob);
			return;
		}

		mob.setSpeed(speed);
	}

	@Unique
	private boolean waterworld$shouldBlockTowardWanted(Mob mob) {
		if (!WaterworldDetection.isActive()) return false;
		if (!WaterworldMobTypes.isHostileBoatPilot(mob)) return false;
		if (mob.getPathfindingMalus(PathType.WATER) >= 0.0F) return false;
		if (mob.isInWater() || mob.isPassenger()) return false;

		double xd = this.wantedX - mob.getX();
		double zd = this.wantedZ - mob.getZ();
		double len = Math.sqrt(xd * xd + zd * zd);
		if (len < 1.0E-4) return false;

		double nx = xd / len;
		double nz = zd / len;
		Level level = mob.level();
		for (double dist : PROBE_DISTANCES) {
			BlockPos ahead = BlockPos.containing(
					mob.getX() + nx * dist,
					mob.getBlockY(),
					mob.getZ() + nz * dist);
			if (waterworld$isWaterHazard(level, ahead)) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private static void waterworld$stopHorizontal(Mob mob) {
		mob.setZza(0.0F);
		mob.setXxa(0.0F);
		Vec3 v = mob.getDeltaMovement();
		mob.setDeltaMovement(0.0, v.y, 0.0);
	}

	/**
	 * Open-water columns are hazards. Full-cube deck is always safe.
	 * Partial footing (stair/slab/shelf/trapdoor) is a hazard only when water
	 * is below it or in a cardinal neighbor column (hull lip).
	 */
	@Unique
	private static boolean waterworld$isWaterHazard(Level level, BlockPos start) {
		BlockPos support = null;
		for (int dy = 0; dy >= -HAZARD_DEPTH; dy--) {
			BlockPos check = start.offset(0, dy, 0);
			if (level.getFluidState(check).is(FluidTags.WATER)) {
				return true;
			}
			BlockState state = level.getBlockState(check);
			if (!state.getCollisionShape(level, check).isEmpty()) {
				support = check;
				break;
			}
		}
		if (support == null) {
			return false;
		}

		BlockState supportState = level.getBlockState(support);
		if (supportState.isCollisionShapeFullBlock(level, support)) {
			return false;
		}

		// Partial lip: water under it, or ocean in an adjacent column.
		if (waterworld$columnHasWaterBeforeSolid(level, support.below(), HAZARD_DEPTH)) {
			return true;
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (waterworld$columnHasWaterBeforeSolid(level, support.relative(dir), HAZARD_DEPTH)) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private static boolean waterworld$columnHasWaterBeforeSolid(Level level, BlockPos from, int depth) {
		for (int dy = 0; dy >= -depth; dy--) {
			BlockPos check = from.offset(0, dy, 0);
			if (level.getFluidState(check).is(FluidTags.WATER)) {
				return true;
			}
			BlockState state = level.getBlockState(check);
			if (!state.getCollisionShape(level, check).isEmpty()) {
				return false;
			}
		}
		return false;
	}
}
