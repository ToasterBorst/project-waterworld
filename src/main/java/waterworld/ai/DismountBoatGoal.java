package waterworld.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

public class DismountBoatGoal extends Goal {
	private static final int LAND_SCAN_RADIUS = 2;
	private static final int MOUNT_COOLDOWN_TICKS = 60;

	private final Mob mob;
	private int cooldown;

	public DismountBoatGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.noneOf(Flag.class));
	}

	@Override
	public boolean canUse() {
		if (cooldown > 0) {
			cooldown--;
			return false;
		}
		return mob.getVehicle() instanceof AbstractBoat boat && shouldDismount(boat);
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		mob.stopRiding();
		resetMountCooldown();
	}

	private void resetMountCooldown() {
		for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
			if (wg.getGoal() instanceof MountNearbyBoatGoal mount) {
				mount.resetCooldown();
				break;
			}
		}
	}

	/**
	 * Called externally after the mob mounts a boat to suppress
	 * immediate re-dismount.
	 */
	public void resetCooldown() {
		this.cooldown = MOUNT_COOLDOWN_TICKS;
	}

	private boolean shouldDismount(AbstractBoat boat) {
		if (boat.getStatus() == AbstractBoat.Status.ON_LAND) return true;
		if (boat.getStatus() != AbstractBoat.Status.IN_WATER) return false;
		if (hasReasonToStayInBoat()) return false;
		return hasNearbyLand(boat);
	}

	/**
	 * Stay in the boat if the mob's target is NOT standing on solid ground
	 * (i.e. the target is in water, on a boat, or airborne -- unreachable
	 * on foot). Also stay if a wandering trader has waterborne threats.
	 */
	private boolean hasReasonToStayInBoat() {
		LivingEntity target = mob.getTarget();
		if (target != null && target.isAlive() && !target.onGround()) return true;

		if (mob instanceof WanderingTrader) {
			AABB threatBox = mob.getBoundingBox().inflate(16.0);
			return !mob.level().getEntitiesOfClass(Monster.class, threatBox,
					m -> m.isAlive() && m.getTarget() == mob && !m.onGround()).isEmpty();
		}
		return false;
	}

	private boolean hasNearbyLand(AbstractBoat boat) {
		Level level = boat.level();
		BlockPos boatPos = boat.blockPosition();
		for (int dx = -LAND_SCAN_RADIUS; dx <= LAND_SCAN_RADIUS; dx++) {
			for (int dz = -LAND_SCAN_RADIUS; dz <= LAND_SCAN_RADIUS; dz++) {
				for (int dy = 0; dy <= 2; dy++) {
					BlockPos check = boatPos.offset(dx, dy, dz);
					if (isStandableLand(level, check)) return true;
				}
			}
		}
		return false;
	}

	private static boolean isStandableLand(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		BlockState at = level.getBlockState(pos);
		BlockState above = level.getBlockState(pos.above());
		return below.isSolid() && !at.isSolid() && !above.isSolid();
	}
}
