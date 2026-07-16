package waterworld.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Steers a boat away from nearby hostile mobs. Used by wandering
 * traders to flee from threats while at sea.
 *
 * Scans for monsters within a 16-block radius and steers in the
 * opposite direction from the nearest one.
 */
public class BoatFleeGoal extends Goal {
	private static final double SCAN_RANGE = 16.0;
	private static final double SAFE_RANGE_SQ = 24.0 * 24.0;
	private static final int RESCAN_INTERVAL = 10;

	private final Mob mob;
	private LivingEntity threat;
	private int rescanTimer;

	public BoatFleeGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return mob.getVehicle() instanceof AbstractBoat;
	}

	@Override
	public boolean canUse() {
		if (!(mob.getVehicle() instanceof AbstractBoat)) return false;
		threat = findNearestThreat();
		return threat != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (!(mob.getVehicle() instanceof AbstractBoat)) return false;
		if (threat == null || !threat.isAlive()) return false;
		return mob.distanceToSqr(threat) < SAFE_RANGE_SQ;
	}

	@Override
	public void start() {
		rescanTimer = 0;
	}

	@Override
	public void stop() {
		if (mob.getVehicle() instanceof AbstractBoat boat) {
			boat.setInput(false, false, false, false);
		}
		threat = null;
	}

	@Override
	public void tick() {
		if (!(mob.getVehicle() instanceof AbstractBoat boat)) return;

		if (--rescanTimer <= 0) {
			LivingEntity newThreat = findNearestThreat();
			if (newThreat != null) threat = newThreat;
			rescanTimer = RESCAN_INTERVAL;
		}

		if (threat == null || !threat.isAlive()) return;

		double dx = mob.getX() - threat.getX();
		double dz = mob.getZ() - threat.getZ();

		float fleeYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float yawDiff = Mth.degreesDifference(boat.getYRot(), fleeYaw);

		boat.setInput(
				yawDiff < -5,
				yawDiff > 5,
				true,
				false
		);
	}

	private LivingEntity findNearestThreat() {
		AABB scanBox = mob.getBoundingBox().inflate(SCAN_RANGE);
		List<Monster> hostiles = mob.level().getEntitiesOfClass(Monster.class, scanBox,
				m -> m.isAlive() && m.distanceToSqr(mob) < SCAN_RANGE * SCAN_RANGE);

		Monster nearest = null;
		double nearestDistSq = Double.MAX_VALUE;
		for (Monster m : hostiles) {
			double d = m.distanceToSqr(mob);
			if (d < nearestDistSq) {
				nearestDistSq = d;
				nearest = m;
			}
		}
		return nearest;
	}
}
