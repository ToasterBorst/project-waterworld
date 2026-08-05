package waterworld.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldDetection;
import waterworld.spawn.SpawnBoatCleanup;

/**
 * Server boat control and distance purge for abandoned mod-spawned rafts.
 * Distance purge lives on {@code tick} because boats never call
 * {@link Entity#checkDespawn()}.
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
		if (!(boat.getControllingPassenger() instanceof Mob)) return;

		boat.controlBoat();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void waterworld$distancePurgeSpawnBoats(CallbackInfo ci) {
		SpawnBoatCleanup.checkDespawn((AbstractBoat) (Object) this);
	}
}
