package waterworld.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Steers a boat toward random water waypoints. Only assigned to illagers
 * and wandering traders -- never to drowned or ravagers.
 */
public class PilotBoatGoal extends Goal {
	private static final double WAYPOINT_REACH_DIST = 4.0;
	private static final double WANDER_RADIUS = 48.0;

	private final Mob mob;
	private Vec3 targetPos;
	private int retargetCooldown;

	public PilotBoatGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return mob.getVehicle() instanceof AbstractBoat;
	}

	@Override
	public boolean canContinueToUse() {
		return mob.getVehicle() instanceof AbstractBoat;
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
			targetPos = pickRandomWaterTarget();
			retargetCooldown = 200 + mob.getRandom().nextInt(200);
		}

		double dx = targetPos.x - boat.getX();
		double dz = targetPos.z - boat.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);

		if (dist < WAYPOINT_REACH_DIST) {
			targetPos = null;
			boat.setInput(false, false, false, false);
			return;
		}

		float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float yawDiff = Mth.degreesDifference(boat.getYRot(), desiredYaw);

		boat.setInput(
			yawDiff < -5,
			yawDiff > 5,
			dist > WAYPOINT_REACH_DIST,
			false
		);
	}

	private Vec3 pickRandomWaterTarget() {
		double angle = mob.getRandom().nextDouble() * Math.PI * 2;
		double radius = WANDER_RADIUS * 0.5 + mob.getRandom().nextDouble() * WANDER_RADIUS * 0.5;
		double x = mob.getX() + Math.cos(angle) * radius;
		double z = mob.getZ() + Math.sin(angle) * radius;
		return new Vec3(x, mob.getY(), z);
	}
}
