package waterworld.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldAttachments;

/**
 * Raider overrides {@code requiresCustomPersistence}; pillagers need the same
 * spawn-boat exception while still persisting during an active raid.
 */
@Mixin(Raider.class)
public class RaiderPersistenceMixin {

	@Inject(method = "requiresCustomPersistence", at = @At("HEAD"), cancellable = true)
	private void waterworld$spawnBoatRaiderPersistence(CallbackInfoReturnable<Boolean> cir) {
		Raider raider = (Raider) (Object) this;
		if (!raider.isPassenger()) return;
		if (WaterworldAttachments.passengerMayDespawn((Entity) raider)) {
			cir.setReturnValue(raider.getCurrentRaid() != null);
		}
	}
}
