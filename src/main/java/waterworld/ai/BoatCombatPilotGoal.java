package waterworld.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
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
 * Only activates when the mob is riding a boat AND has an attack target.
 * Falls through to PilotBoatGoal (random wander) when there is no target.
 */
public class BoatCombatPilotGoal extends Goal {
	private static final double RANGED_ATTACK_RANGE_SQ = 15.0 * 15.0;
	private static final double MELEE_ATTACK_RANGE_SQ = 3.5 * 3.5;
	private static final double APPROACH_DIST = 4.0;
	private static final int MELEE_COOLDOWN_TICKS = 20;

	private final Mob mob;
	private int meleeTimer;
	private ChargeState chargeState = ChargeState.UNCHARGED;

	private enum ChargeState { UNCHARGED, CHARGING, CHARGED }

	public BoatCombatPilotGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
		chargeState = ChargeState.UNCHARGED;
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
		chargeState = ChargeState.UNCHARGED;
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null || !target.isAlive()) return;
		if (!(mob.getVehicle() instanceof AbstractBoat boat)) return;

		double distSq = mob.distanceToSqr(target);

		mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

		steerToward(boat, target.getX(), target.getZ());

		if (isCrossbowUser()) {
			handleCrossbowCombat(target, distSq);
		} else {
			handleMeleeCombat(target, distSq);
		}
	}

	private void steerToward(AbstractBoat boat, double tx, double tz) {
		double dx = tx - boat.getX();
		double dz = tz - boat.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);

		float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
		float yawDiff = Mth.degreesDifference(boat.getYRot(), desiredYaw);

		boat.setInput(
				yawDiff < -5,
				yawDiff > 5,
				dist > APPROACH_DIST,
				false
		);
	}

	private boolean isCrossbowUser() {
		return mob instanceof RangedAttackMob
				&& mob.getMainHandItem().is(Items.CROSSBOW);
	}

	private void handleCrossbowCombat(LivingEntity target, double distSq) {
		if (distSq > RANGED_ATTACK_RANGE_SQ) {
			if (mob.isUsingItem()) {
				mob.stopUsingItem();
				chargeState = ChargeState.UNCHARGED;
				if (mob instanceof CrossbowAttackMob cbm) cbm.setChargingCrossbow(false);
			}
			return;
		}

		ItemStack crossbow = mob.getMainHandItem();

		switch (chargeState) {
			case UNCHARGED -> {
				mob.startUsingItem(InteractionHand.MAIN_HAND);
				if (mob instanceof CrossbowAttackMob cbm) cbm.setChargingCrossbow(true);
				chargeState = ChargeState.CHARGING;
			}
			case CHARGING -> {
				if (!mob.isUsingItem()) {
					if (mob instanceof CrossbowAttackMob cbm) cbm.setChargingCrossbow(false);
					if (CrossbowItem.isCharged(crossbow)) {
						chargeState = ChargeState.CHARGED;
					} else {
						chargeState = ChargeState.UNCHARGED;
					}
				}
			}
			case CHARGED -> {
				if (mob instanceof RangedAttackMob ranged) {
					ranged.performRangedAttack(target, 1.0f);
				}
				if (mob instanceof CrossbowAttackMob cbm) cbm.onCrossbowAttackPerformed();
				chargeState = ChargeState.UNCHARGED;
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
