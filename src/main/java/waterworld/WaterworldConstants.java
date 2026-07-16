package waterworld;

public final class WaterworldConstants {
	public static final int SEA_LEVEL = 112;

	/** Soft density fade starts here (full land contribution at/below). */
	public static final int SEA_BED_FADE_START_Y = 23;

	/** Soft density fade ends here — typical solid seabed tops under the water plane. */
	public static final int SEA_BED_MAX_Y = 83;

	private WaterworldConstants() {
	}
}
