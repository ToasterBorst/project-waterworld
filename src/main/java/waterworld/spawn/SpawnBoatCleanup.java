package waterworld.spawn;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import waterworld.WaterworldAttachments;

/**
 * Purges abandoned mod-spawned rafts via distance (from boat tick) and chunk unload.
 * Player boats (untagged), chest boats, leashed boats, and occupied boats are kept.
 */
public final class SpawnBoatCleanup {
	/** Same threshold as {@code MobCategory.MONSTER} despawn distance. */
	private static final double PURGE_DISTANCE_SQ = 128.0 * 128.0;

	private SpawnBoatCleanup() {
	}

	/**
	 * Call from {@link AbstractBoat#tick()}. Empty tagged rafts despawn when no
	 * player is within 128 blocks (same rule as hostile mobs). Early-outs before
	 * any player lookup unless the boat is a purgeable spawn raft.
	 */
	public static void checkDespawn(AbstractBoat boat) {
		if (boat.level().isClientSide()) return;
		if (!WaterworldAttachments.isSpawnBoat(boat)) return;
		if (!WaterworldAttachments.isPurgeableSpawnBoat(boat)) return;

		Player nearest = boat.level().getNearestPlayer(boat, -1.0);
		if (nearest == null || nearest.distanceToSqr(boat) > PURGE_DISTANCE_SQ) {
			boat.discard();
		}
	}
}
