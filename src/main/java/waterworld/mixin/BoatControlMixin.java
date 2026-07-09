package waterworld.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

/**
 * Vanilla boats only call controlBoat() on the client side, so
 * setInput() steering flags are never processed on the server.
 * This mixin injects a server-side controlBoat() call right before
 * the boat applies movement, allowing mob pilots to actually steer.
 */
@Mixin(AbstractBoat.class)
public class BoatControlMixin {

	@Inject(method = "tick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
	private void waterworld$serverControlBoat(CallbackInfo ci) {
		AbstractBoat boat = (AbstractBoat) (Object) this;
		if (boat.level().isClientSide()) return;
		if (!WaterworldDetection.isActive()) return;
		if (!WaterworldConfig.INSTANCE.mobsCanPilotBoats) return;
		if (!(boat.getControllingPassenger() instanceof Mob)) return;

		boat.controlBoat();
	}
}
