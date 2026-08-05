package waterworld;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;

/**
 * Persistent tags for mod-spawned rafts: passenger despawn + abandoned-boat purge.
 * Player-placed boats stay untagged and are never auto-purged.
 */
public final class WaterworldAttachments {
	public static final AttachmentType<Boolean> SPAWN_BOAT = AttachmentRegistry.createPersistent(
			Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "spawn_boat"),
			Codec.BOOL);

	private WaterworldAttachments() {
	}

	public static void markSpawnBoat(AbstractBoat boat) {
		boat.setAttached(SPAWN_BOAT, true);
	}

	public static boolean isSpawnBoat(Entity entity) {
		return entity instanceof AbstractBoat boat
				&& Boolean.TRUE.equals(boat.getAttached(SPAWN_BOAT));
	}

	/**
	 * Empty mod-spawned non-chest unleased boats may be discarded on unload / distance.
	 */
	public static boolean isPurgeableSpawnBoat(AbstractBoat boat) {
		if (!WaterworldDetection.isActive()) return false;
		if (!isSpawnBoat(boat)) return false;
		if (boat instanceof AbstractChestBoat) return false;
		if (boat.isLeashed()) return false;
		return boat.getPassengers().isEmpty();
	}

	/**
	 * Passengers on mod-spawned rafts may use vanilla despawn rules again.
	 */
	public static boolean passengerMayDespawn(Entity passenger) {
		if (!WaterworldDetection.isActive()) return false;
		if (!passenger.isPassenger()) return false;
		return isSpawnBoat(passenger.getVehicle());
	}
}
