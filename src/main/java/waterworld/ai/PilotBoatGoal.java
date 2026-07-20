package waterworld.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Steers a boat toward meaningful destinations. Tries targets in
 * priority order: nearest player, raid center, patrol target, nearest
 * land, then random wander as a last resort.
 */
public class PilotBoatGoal extends Goal {
	private static final double WAYPOINT_REACH_DIST = 4.0;
	private static final double PLAYER_SEARCH_RANGE = 64.0;
	private static final double LAND_SEARCH_RANGE = 48.0;
	private static final double WANDER_RADIUS = 48.0;
	private static final int RETARGET_INTERVAL = 100;
	private static final int WAYPOINT_REACHED_DELAY = 20;

	private final Mob mob;
	private Vec3 targetPos;
	private int retargetCooldown;

	public PilotBoatGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		// Boat input must refresh every tick while piloting; idle when not aboard.
		return isControllingBoat();
	}

	@Override
	public boolean canUse() {
		return isControllingBoat();
	}

	@Override
	public boolean canContinueToUse() {
		return isControllingBoat();
	}

	private boolean isControllingBoat() {
		return mob.getVehicle() instanceof AbstractBoat boat
				&& boat.getControllingPassenger() == mob;
	}

	@Override
	public void start() {
		targetPos = null;
		retargetCooldown = 0;
	}

	@Override
	public void stop() {
		if (mob.getVehicle() instanceof AbstractBoat boat) {
			boat.setInput(false, false, false, false);
		}
	}

	@Override
	public void tick() {
		if (!(mob.getVehicle() instanceof AbstractBoat boat)) return;

		if (targetPos == null || --retargetCooldown <= 0) {
			targetPos = pickSmartTarget();
			retargetCooldown = RETARGET_INTERVAL + mob.getRandom().nextInt(60);
		}

		double dx = targetPos.x - boat.getX();
		double dz = targetPos.z - boat.getZ();
		double distSq = dx * dx + dz * dz;

		if (distSq < WAYPOINT_REACH_DIST * WAYPOINT_REACH_DIST) {
			targetPos = null;
			retargetCooldown = WAYPOINT_REACHED_DELAY;
			boat.setInput(false, false, false, false);
			return;
		}

		float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float yawDiff = Mth.degreesDifference(boat.getYRot(), desiredYaw);

		boat.setInput(
			yawDiff < -5,
			yawDiff > 5,
			true,
			false
		);
	}

	private Vec3 pickSmartTarget() {
		Vec3 result;

		result = findNearestPlayer();
		if (result != null) return result;

		result = findRaidCenter();
		if (result != null) return result;

		result = findPatrolTarget();
		if (result != null) return result;

		result = findNearestLand();
		if (result != null) return result;

		return pickRandomWaterTarget();
	}

	private Vec3 findNearestPlayer() {
		Player nearest = mob.level().getNearestPlayer(mob, PLAYER_SEARCH_RANGE);
		return nearest != null ? nearest.position() : null;
	}

	private Vec3 findRaidCenter() {
		if (mob instanceof Raider raider && raider.hasActiveRaid()
				&& raider.getCurrentRaid() != null) {
			BlockPos center = raider.getCurrentRaid().getCenter();
			return Vec3.atCenterOf(center);
		}
		return null;
	}

	private Vec3 findPatrolTarget() {
		if (mob instanceof PatrollingMonster patrol && patrol.hasPatrolTarget()) {
			return Vec3.atCenterOf(patrol.getPatrolTarget());
		}
		return null;
	}

	private Vec3 findNearestLand() {
		Level level = mob.level();
		BlockPos origin = mob.blockPosition();
		int range = (int) LAND_SEARCH_RANGE;

		BlockPos nearest = null;
		double nearestDistSq = Double.MAX_VALUE;

		for (int attempt = 0; attempt < 20; attempt++) {
			int dx = mob.getRandom().nextInt(range * 2 + 1) - range;
			int dz = mob.getRandom().nextInt(range * 2 + 1) - range;
			BlockPos column = origin.offset(dx, 0, dz);

			for (int dy = -2; dy <= 4; dy++) {
				BlockPos check = column.atY(origin.getY() + dy);
				BlockState below = level.getBlockState(check.below());
				BlockState at = level.getBlockState(check);
				if (below.isSolid() && !at.isSolid()) {
					double distSq = check.distSqr(origin);
					if (distSq < nearestDistSq) {
						nearestDistSq = distSq;
						nearest = check;
					}
					break;
				}
			}
		}

		return nearest != null ? Vec3.atCenterOf(nearest) : null;
	}

	private Vec3 pickRandomWaterTarget() {
		double angle = mob.getRandom().nextDouble() * Math.PI * 2;
		double radius = WANDER_RADIUS * 0.5 + mob.getRandom().nextDouble() * WANDER_RADIUS * 0.5;
		double x = mob.getX() + Math.cos(angle) * radius;
		double z = mob.getZ() + Math.sin(angle) * radius;
		return new Vec3(x, mob.getY(), z);
	}
}
