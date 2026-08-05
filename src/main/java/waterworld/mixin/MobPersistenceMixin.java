package waterworld.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldAttachments;

/**
 * Restores vanilla despawn for passengers on mod-spawned rafts.
 * Player boat traps remain persistent (untagged boats).
 */
@Mixin(Mob.class)
public class MobPersistenceMixin {

	@Inject(method = "requiresCustomPersistence", at = @At("HEAD"), cancellable = true)
	private void waterworld$spawnBoatPassengerPersistence(CallbackInfoReturnable<Boolean> cir) {
		Mob self = (Mob) (Object) this;
		if (!self.isPassenger()) return;
		if (WaterworldAttachments.passengerMayDespawn((Entity) self)) {
			cir.setReturnValue(false);
		}
	}
}
