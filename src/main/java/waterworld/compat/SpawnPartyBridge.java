package waterworld.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft checks against Spawn Party for JOIN gear deferral.
 * Placement content is applied via {@link SpawnPartyOriginHook} (called from Spawn Party).
 */
public final class SpawnPartyBridge {

	private static final Logger LOGGER = LoggerFactory.getLogger("waterworld/spawnparty");
	private static final String MOD_ID = "spawnparty";

	private static Boolean present;
	private static Method isHolding;
	private static Method hasCompletedFirstPlacement;
	private static boolean apiBound;
	private static boolean apiBindAttempted;

	private SpawnPartyBridge() {
	}

	public static boolean isSpawnPartyPresent() {
		if (present == null) {
			present = FabricLoader.getInstance().isModLoaded(MOD_ID);
		}
		return present;
	}

	/**
	 * True when Spawn Party is holding this player or they have not finished first-origin placement yet.
	 * Fail-closed: if Spawn Party is loaded but API cannot be bound, still defer.
	 */
	public static boolean shouldDeferJoinGear(UUID playerId) {
		if (!isSpawnPartyPresent()) return false;
		ensureApiMethods();
		if (!apiBound) {
			LOGGER.warn("Spawn Party present but API unbound — deferring spawn gear (fail-closed)");
			return true;
		}
		try {
			if (Boolean.TRUE.equals(isHolding.invoke(null, playerId))) {
				return true;
			}
			if (!Boolean.TRUE.equals(hasCompletedFirstPlacement.invoke(null, playerId))) {
				return true;
			}
		} catch (Throwable t) {
			LOGGER.warn("Spawn Party API probe failed — deferring spawn gear", t);
			return true;
		}
		return false;
	}

	/** No-op: placement is driven by Spawn Party calling {@link SpawnPartyOriginHook}. */
	public static void register() {
		if (!isSpawnPartyPresent()) {
			LOGGER.info("Spawn Party not present — Waterworld uses JOIN spawn gear only");
			return;
		}
		ensureApiMethods();
		LOGGER.info("Spawn Party soft-bridge ready (placement hook + JOIN defer)");
	}

	private static void ensureApiMethods() {
		if (apiBindAttempted) return;
		apiBindAttempted = true;
		if (!isSpawnPartyPresent()) return;
		try {
			Class<?> api = Class.forName("spawnparty.api.SpawnPartyAPI");
			isHolding = api.getMethod("isHolding", UUID.class);
			hasCompletedFirstPlacement = api.getMethod("hasCompletedFirstPlacement", UUID.class);
			apiBound = true;
		} catch (Throwable t) {
			apiBound = false;
			LOGGER.warn("Could not bind SpawnPartyAPI", t);
		}
	}
}
