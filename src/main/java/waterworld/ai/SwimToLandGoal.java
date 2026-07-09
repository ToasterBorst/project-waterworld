package waterworld.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Pathfinds toward the nearest standable land when the mob is in
 * water and not riding a boat. Ensures mobs don't idle in the
 * ocean after dismounting.
 */
public class SwimToLandGoal extends Goal {
	private static final int SCAN_RANGE = 16;
	private static final int RESCAN_INTERVAL = 40;

	private final Mob mob;
	private BlockPos landTarget;
	private int rescanTimer;

	public SwimToLandGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (mob.isPassenger()) return false;
		if (!mob.isInWater()) return false;
		landTarget = findNearestLand();
		return landTarget != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (mob.isPassenger()) return false;
		if (!mob.isInWater()) return false;
		return landTarget != null;
	}

	@Override
	public void start() {
		rescanTimer = 0;
		navigateToLand();
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		landTarget = null;
	}

	@Override
	public void tick() {
		if (--rescanTimer <= 0) {
			BlockPos better = findNearestLand();
			if (better != null) landTarget = better;
			rescanTimer = RESCAN_INTERVAL;
			navigateToLand();
		}

		if (mob.getNavigation().isDone() && landTarget != null) {
			navigateToLand();
		}
	}

	private void navigateToLand() {
		if (landTarget != null) {
			mob.getNavigation().moveTo(
					landTarget.getX() + 0.5,
					landTarget.getY(),
					landTarget.getZ() + 0.5,
					1.0);
		}
	}

	private BlockPos findNearestLand() {
		Level level = mob.level();
		BlockPos origin = mob.blockPosition();

		BlockPos nearest = null;
		double nearestDistSq = Double.MAX_VALUE;

		for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx += 2) {
			for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz += 2) {
				for (int dy = -2; dy <= 3; dy++) {
					BlockPos check = origin.offset(dx, dy, dz);
					BlockState below = level.getBlockState(check.below());
					BlockState at = level.getBlockState(check);
					BlockState above = level.getBlockState(check.above());
					if (below.isSolid() && !at.isSolid() && !above.isSolid()) {
						double distSq = check.distSqr(origin);
						if (distSq < nearestDistSq) {
							nearestDistSq = distSq;
							nearest = check;
						}
						break;
					}
				}
			}
		}
		return nearest;
	}
}
