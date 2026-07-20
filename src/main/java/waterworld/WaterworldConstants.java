package waterworld;

public final class WaterworldConstants {
	/** Default ocean waterline; override via {@code sea_level} in waterworld.properties. */
	public static final int DEFAULT_SEA_LEVEL = 101;

	/** Soft density fade starts here (full land contribution at/below). */
	public static final int SEA_BED_FADE_START_Y = 23;

	/** Soft density fade ends here — typical solid seabed tops under the water plane. */
	public static final int SEA_BED_MAX_Y = 83;

	private WaterworldConstants() {
	}

	/**
	 * Effective sea level for structures, biomes, and spawn helpers.
	 * Must stay in sync with {@code data/.../noise_settings/waterworld.json} for new worlds.
	 */
	public static int seaLevel() {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		return config != null ? config.seaLevel : DEFAULT_SEA_LEVEL;
	}
}
