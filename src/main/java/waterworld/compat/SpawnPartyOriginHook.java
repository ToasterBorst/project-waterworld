package waterworld.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.spawn.PartyIslandTracker;
import waterworld.spawn.SpawnGearHandler;
import waterworld.spawn.SpawnIslandGenerator;

import java.util.UUID;

/**
 * Called by Spawn Party via reflection after first-origin placement.
 * Soft bridge — Spawn Party does not hard-depend on Waterworld.
 */
public final class SpawnPartyOriginHook {

	private static final Logger LOGGER = LoggerFactory.getLogger("waterworld/spawnparty");

	private SpawnPartyOriginHook() {
	}

	/**
	 * @param player  player who just reached party origin
	 * @param partyId Spawn Party party UUID (island keyed once per party)
	 * @param origin  party spawn block position
	 */
	public static void onFirstOriginPlacement(ServerPlayer player, UUID partyId, BlockPos origin) {
		LOGGER.info("First-origin placement for {} party={} at {}",
				player.getName().getString(), partyId, origin);

		if (!WaterworldDetection.isActive()) {
			LOGGER.info("Skipping Waterworld spawn content — gameplay not active");
			return;
		}

		WaterworldConfig config = WaterworldConfig.INSTANCE;
		BlockPos gearNear = origin;

		if (config.spawnIsland && partyId != null) {
			if (PartyIslandTracker.hasIsland(partyId)) {
				LOGGER.info("Party {} already has a spawn island — skipping generate", partyId);
			} else {
				try {
					int y = SpawnIslandGenerator.generate(player.level(), origin.getX(), origin.getZ());
					gearNear = new BlockPos(origin.getX(), y, origin.getZ());
					PartyIslandTracker.markIsland(partyId);
					LOGGER.info("Generated spawn island for party {} at {},{},{}",
							partyId, gearNear.getX(), gearNear.getY(), gearNear.getZ());
				} catch (Exception e) {
					LOGGER.warn("Per-party spawn island failed; continuing with gear only", e);
				}
			}
		}

		if (config.spawnGear) {
			final BlockPos gearAt = gearNear;
			player.level().getServer().execute(() -> SpawnGearHandler.giveSpawnGearAt(player, gearAt));
		}
	}
}
