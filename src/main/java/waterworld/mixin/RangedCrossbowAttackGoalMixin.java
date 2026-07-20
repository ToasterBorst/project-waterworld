package waterworld.mixin;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import waterworld.WaterworldDetection;
import waterworld.spawn.WaterworldMobTypes;

/**
 * When a crossbow illager's target is swimming or riding a waterborne boat,
 * hold position and shoot instead of {@code moveTo}-ing toward the deck edge.
 * Boat passengers are not {@code isInWater()} while floating, so the vehicle
 * must be checked separately.
 */
@Mixin(RangedCrossbowAttackGoal.class)
public abstract class RangedCrossbowAttackGoalMixin {
	@Shadow
	@Final
	private Monster mob;

	@ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 2)
	private boolean waterworld$standAndShootInWater(boolean needsToMove) {
		if (!needsToMove) return false;
		if (!waterworld$shouldHoldDeck()) return needsToMove;

		this.mob.getNavigation().stop();
		return false;
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"
			)
	)
	private boolean waterworld$noPathToSwimmer(PathNavigation navigation, Entity target, double speed) {
		if (waterworld$shouldHoldDeck()) {
			navigation.stop();
			return false;
		}
		return navigation.moveTo(target, speed);
	}

	@Unique
	private boolean waterworld$shouldHoldDeck() {
		if (!WaterworldDetection.isActive()) return false;
		if (!WaterworldMobTypes.isHostileBoatPilot(this.mob)) return false;
		LivingEntity target = this.mob.getTarget();
		if (target == null) return false;
		if (target.isInWater() || target.getFluidHeight(FluidTags.WATER) > 0.0) return true;

		Entity vehicle = target.getVehicle();
		return vehicle instanceof AbstractBoat
				&& (vehicle.isInWater() || vehicle.getFluidHeight(FluidTags.WATER) > 0.0);
	}
}
