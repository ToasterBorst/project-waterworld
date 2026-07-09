package waterworld.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * Steers a boat toward the mob's attack target and performs attacks
 * when in range. Crossbow users (pillagers) load and fire from the
 * boat; melee mobs (vindicators) close distance and swing.
 *
 * The crossbow state machine mirrors vanilla's RangedCrossbowAttackGoal:
 * UNCHARGED -> CHARGING -> CHARGED (delay) -> READY_TO_ATTACK -> fire -> UNCHARGED
 *
 * Only activates when the mob is riding a boat AND has an attack target.
 * Falls through to PilotBoatGoal (random wander) when there is no target.
 */
public class BoatCombatPilotGoal extends Goal {
	private static final double RANGED_ATTACK_RANGE_SQ = 15.0 * 15.0;
	private static final double RANGED_STOP_DIST = 12.0;
	private static final double MELEE_ATTACK_RANGE_SQ = 3.5 * 3.5;
	private static final double MELEE_STOP_DIST = 4.0;
	private static final int MELEE_COOLDOWN_TICKS = 20;
	private static final int LOS_TIMEOUT_TICKS = 60;

	private final Mob mob;
	private int meleeTimer;
	private int attackDelay;
	private int noLosTimer;
	private CrossbowState crossbowState = CrossbowState.UNCHARGED;

	private enum CrossbowState { UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK }

	public BoatCombatPilotGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		return mob.getVehicle() instanceof AbstractBoat
				&& mob.getTarget() != null
				&& mob.getTarget().isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		meleeTimer = 0;
		attackDelay = 0;
		noLosTimer = 0;
		crossbowState = CrossbowState.UNCHARGED;
	}

	@Override
	public void stop() {
		if (mob.getVehicle() instanceof AbstractBoat boat) {
			boat.setInput(false, false, false, false);
		}
		if (mob.isUsingItem()) {
			mob.stopUsingItem();
		}
		if (mob instanceof CrossbowAttackMob cbm) {
			cbm.setChargingCrossbow(false);
		}
		crossbowState = CrossbowState.UNCHARGED;
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null || !target.isAlive()) return;
		if (!(mob.getVehicle() instanceof AbstractBoat boat)) return;

		double distSq = mob.distanceToSqr(target);

		mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

		if (isCrossbowUser()) {
			boolean inRange = distSq <= RANGED_ATTACK_RANGE_SQ;
			boolean canRow = crossbowState == CrossbowState.UNCHARGED;
			double stopDist = canRow ? RANGED_STOP_DIST : 0;
			steerToward(boat, target.getX(), target.getZ(), stopDist, canRow || !inRange);
			handleCrossbowCombat(target, distSq);
		} else {
			steerToward(boat, target.getX(), target.getZ(), MELEE_STOP_DIST, true);
			handleMeleeCombat(target, distSq);
		}
	}

	private void steerToward(AbstractBoat boat, double tx, double tz, double stopDist, boolean allowForward) {
		double dx = tx - boat.getX();
		double dz = tz - boat.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);

		float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float yawDiff = Mth.degreesDifference(boat.getYRot(), desiredYaw);

		boat.setInput(
				yawDiff < -5,
				yawDiff > 5,
				allowForward && dist > stopDist,
				false
		);
	}

	private boolean isCrossbowUser() {
		return mob instanceof RangedAttackMob && mob.isHolding(Items.CROSSBOW);
	}

	private void handleCrossbowCombat(LivingEntity target, double distSq) {
		if (distSq > RANGED_ATTACK_RANGE_SQ) {
			if (mob.isUsingItem()) {
				mob.stopUsingItem();
				if (mob instanceof CrossbowAttackMob cbm) cbm.setChargingCrossbow(false);
			}
			crossbowState = CrossbowState.UNCHARGED;
			return;
		}

		switch (crossbowState) {
			case UNCHARGED -> {
				InteractionHand hand = ProjectileUtil.getWeaponHoldingHand(mob, Items.CROSSBOW);
				mob.startUsingItem(hand);
				if (mob instanceof CrossbowAttackMob cbm) cbm.setChargingCrossbow(true);
				crossbowState = CrossbowState.CHARGING;
			}
			case CHARGING -> {
				if (!mob.isUsingItem()) {
					crossbowState = CrossbowState.UNCHARGED;
					return;
				}
				ItemStack useItem = mob.getUseItem();
				int ticksUsing = mob.getTicksUsingItem();
				if (ticksUsing >= CrossbowItem.getChargeDuration(useItem, mob)) {
					mob.releaseUsingItem();
					if (mob instanceof CrossbowAttackMob cbm) cbm.setChargingCrossbow(false);
					crossbowState = CrossbowState.CHARGED;
					attackDelay = 20 + mob.getRandom().nextInt(20);
				}
			}
			case CHARGED -> {
				if (--attackDelay <= 0) {
					crossbowState = CrossbowState.READY_TO_ATTACK;
				}
			}
			case READY_TO_ATTACK -> {
				if (mob.getSensing().hasLineOfSight(target)) {
					noLosTimer = 0;
					if (mob instanceof RangedAttackMob ranged) {
						ranged.performRangedAttack(target, 1.0f);
					}
					if (mob instanceof CrossbowAttackMob cbm) cbm.onCrossbowAttackPerformed();
					crossbowState = CrossbowState.UNCHARGED;
				} else if (++noLosTimer > LOS_TIMEOUT_TICKS) {
					noLosTimer = 0;
					crossbowState = CrossbowState.UNCHARGED;
				}
			}
		}
	}

	private void handleMeleeCombat(LivingEntity target, double distSq) {
		if (distSq > MELEE_ATTACK_RANGE_SQ) return;
		if (--meleeTimer > 0) return;

		if (mob.level() instanceof ServerLevel serverLevel) {
			mob.doHurtTarget(serverLevel, target);
		}
		meleeTimer = MELEE_COOLDOWN_TICKS;
	}
}
