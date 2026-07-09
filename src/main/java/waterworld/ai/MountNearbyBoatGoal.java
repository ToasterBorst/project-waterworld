package waterworld.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * When a mob is in water and not riding anything, scans for a
 * nearby boat with room and boards it. Includes a cooldown after
 * dismount to prevent rapid mount/dismount oscillation.
 */
public class MountNearbyBoatGoal extends Goal {
	private static final double SEARCH_RANGE = 8.0;
	private static final double MOUNT_RANGE_SQ = 2.5 * 2.5;
	private static final int MAX_BOAT_PASSENGERS = 2;
	private static final int REMOUNT_COOLDOWN_TICKS = 60;

	private final Mob mob;
	private AbstractBoat targetBoat;
	private int cooldown;

	public MountNearbyBoatGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		if (mob.isPassenger()) return false;
		if (!mob.isInWater()) return false;
		if (cooldown > 0) {
			cooldown--;
			return false;
		}
		targetBoat = findNearbyBoat();
		return targetBoat != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (mob.isPassenger()) return false;
		if (targetBoat == null || !targetBoat.isAlive()) return false;
		if (targetBoat.getPassengers().size() >= MAX_BOAT_PASSENGERS) return false;
		return true;
	}

	@Override
	public void start() {
		navigateToBoat();
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		targetBoat = null;
	}

	@Override
	public void tick() {
		if (targetBoat == null || !targetBoat.isAlive()) return;

		double distSq = mob.distanceToSqr(targetBoat);

		if (distSq <= MOUNT_RANGE_SQ) {
			mob.startRiding(targetBoat);
			resetDismountCooldown();
			return;
		}

		if (mob.getNavigation().isDone()) {
			navigateToBoat();
		}
	}

	/**
	 * Called externally after the mob dismounts to suppress
	 * immediate re-mount.
	 */
	public void resetCooldown() {
		this.cooldown = REMOUNT_COOLDOWN_TICKS;
	}

	private void resetDismountCooldown() {
		for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
			if (wg.getGoal() instanceof DismountBoatGoal dismount) {
				dismount.resetCooldown();
				break;
			}
		}
	}

	private void navigateToBoat() {
		if (targetBoat != null) {
			mob.getNavigation().moveTo(targetBoat, 1.0);
		}
	}

	private AbstractBoat findNearbyBoat() {
		AABB searchBox = mob.getBoundingBox().inflate(SEARCH_RANGE);
		List<AbstractBoat> boats = mob.level().getEntitiesOfClass(AbstractBoat.class, searchBox,
				this::isEligible);

		AbstractBoat nearest = null;
		double nearestDistSq = Double.MAX_VALUE;
		for (AbstractBoat boat : boats) {
			double d = mob.distanceToSqr(boat);
			if (d < nearestDistSq) {
				nearestDistSq = d;
				nearest = boat;
			}
		}
		return nearest;
	}

	private boolean isEligible(AbstractBoat boat) {
		if (!boat.isAlive()) return false;
		if (boat.getPassengers().size() >= MAX_BOAT_PASSENGERS) return false;
		return !boat.hasPassenger(e -> e instanceof Player);
	}
}
