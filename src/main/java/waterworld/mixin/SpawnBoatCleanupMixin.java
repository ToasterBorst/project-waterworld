package waterworld.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import waterworld.WaterworldAttachments;

/**
 * Unload-without-save for abandoned mod-spawned rafts.
 * {@code setRemoved} is defined on {@link Entity}, so this mixin targets Entity.
 */
@Mixin(Entity.class)
public class SpawnBoatCleanupMixin {

	/**
	 * When a purgeable spawn boat would unload-and-save, discard it instead so it
	 * is not written back to the chunk.
	 */
	@ModifyVariable(method = "setRemoved", at = @At("HEAD"), argsOnly = true)
	private Entity.RemovalReason waterworld$discardPurgeableOnUnload(Entity.RemovalReason reason) {
		if (reason != Entity.RemovalReason.UNLOADED_TO_CHUNK) return reason;
		if ((Object) this instanceof AbstractBoat boat
				&& WaterworldAttachments.isPurgeableSpawnBoat(boat)) {
			return Entity.RemovalReason.DISCARDED;
		}
		return reason;
	}
}
