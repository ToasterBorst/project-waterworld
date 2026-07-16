package waterworld.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import java.util.EnumSet;

/**
 * Steers a boat toward the nearest non-spectator player and holds
 * position at a comfortable trading distance. Used by wandering
 * traders to make themselves accessible to players at sea.
 */
public class BoatApproachPlayerGoal extends Goal {
	private static final double SEARCH_RANGE = 48.0;
	private static final double HOLD_DISTANCE = 8.0;
	private static final double HOLD_DISTANCE_SQ = HOLD_DISTANCE * HOLD_DISTANCE;
	private static final double APPROACH_DIST = 4.0;
	private static final int RESCAN_INTERVAL = 40;

	private final Mob mob;
	private Player targetPlayer;
	private int rescanTimer;

	public BoatApproachPlayerGoal(Mob mob) {
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
		targetPlayer = findNearestPlayer();
		return targetPlayer != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (!(mob.getVehicle() instanceof AbstractBoat)) return false;
		if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isSpectator()) {
			return false;
		}
		return mob.distanceToSqr(targetPlayer) < SEARCH_RANGE * SEARCH_RANGE;
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
		targetPlayer = null;
	}

	@Override
	public void tick() {
		if (!(mob.getVehicle() instanceof AbstractBoat boat)) return;

		if (--rescanTimer <= 0) {
			Player nearer = findNearestPlayer();
			if (nearer != null) targetPlayer = nearer;
			rescanTimer = RESCAN_INTERVAL;
		}

		if (targetPlayer == null) return;

		double distSq = mob.distanceToSqr(targetPlayer);

		if (distSq <= HOLD_DISTANCE_SQ) {
			boat.setInput(false, false, false, false);
			mob.getLookControl().setLookAt(targetPlayer, 30.0f, 30.0f);
			return;
		}

		double dx = targetPlayer.getX() - boat.getX();
		double dz = targetPlayer.getZ() - boat.getZ();

		float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float yawDiff = Mth.degreesDifference(boat.getYRot(), desiredYaw);

		boat.setInput(
				yawDiff < -5,
				yawDiff > 5,
				distSq > APPROACH_DIST * APPROACH_DIST,
				false
		);
	}

	private Player findNearestPlayer() {
		return mob.level().getNearestPlayer(mob, SEARCH_RANGE);
	}
}
