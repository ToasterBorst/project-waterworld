package waterworld.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldMod;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

/**
 * Slows guardians only while a drowned is riding them so boats can escape
 * continuous chase. Bare guardians keep vanilla MOVEMENT_SPEED.
 */
@Mixin(Guardian.class)
public abstract class GuardianMountedSpeedMixin {
	@Unique
	private static final Identifier MOUNTED_SPEED_ID =
			Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "mounted_drowned_slow");

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void waterworld$syncMountedSpeed(CallbackInfo ci) {
		Guardian guardian = (Guardian) (Object) this;
		if (guardian.level().isClientSide()) return;
		if (!WaterworldDetection.isActive()) return;

		double factor = WaterworldConfig.INSTANCE.mountedGuardianSpeedFactor;
		AttributeInstance speed = guardian.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;

		if (factor >= 1.0) {
			if (speed.hasModifier(MOUNTED_SPEED_ID)) {
				speed.removeModifier(MOUNTED_SPEED_ID);
			}
			return;
		}

		boolean shouldSlow = waterworld$hasDrownedPassenger(guardian);

		if (shouldSlow) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(
					MOUNTED_SPEED_ID,
					factor - 1.0,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else if (speed.hasModifier(MOUNTED_SPEED_ID)) {
			speed.removeModifier(MOUNTED_SPEED_ID);
		}
	}

	@Unique
	private static boolean waterworld$hasDrownedPassenger(Guardian guardian) {
		for (Entity passenger : guardian.getPassengers()) {
			if (passenger instanceof Drowned) {
				return true;
			}
		}
		return false;
	}
}
