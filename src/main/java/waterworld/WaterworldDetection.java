package waterworld;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import waterworld.worldgen.WaterworldBiomeSource;

/**
 * Detects whether the current server is running a Waterworld world type
 * and gates mod gameplay effects accordingly.
 *
 * Detection is cached per server lifecycle to avoid repeated generator
 * lookups on every mob tick.
 */
public final class WaterworldDetection {

	private static boolean activeForCurrentServer = false;

	private WaterworldDetection() {
	}

	/**
	 * Called on server start to detect whether the overworld uses our world type.
	 * In "always" mode this is forced true; in "never" mode it's forced false.
	 */
	public static void onServerStarted(MinecraftServer server) {
		String mode = WaterworldConfig.INSTANCE.activationMode.trim().toLowerCase();

		activeForCurrentServer = switch (mode) {
			case "always" -> true;
			case "never" -> false;
			default -> detectWaterworldOverworld(server);
		};

		ProjectWaterworld.LOGGER.info("Waterworld activation: mode={}, active={}",
				mode, activeForCurrentServer);
	}

	/**
	 * Called on server stop to reset cached state.
	 */
	public static void onServerStopped(MinecraftServer server) {
		activeForCurrentServer = false;
	}

	/**
	 * Returns true if mod gameplay effects should be active for the current server.
	 * Safe to call from any thread after server start.
	 */
	public static boolean isActive() {
		return activeForCurrentServer;
	}

	/**
	 * Convenience for mixin use: returns true if effects should be active for
	 * the given level's server. Falls back to the cached value.
	 */
	public static boolean isActive(ServerLevel level) {
		return activeForCurrentServer;
	}

	/**
	 * Direct generator check for use before SERVER_STARTED (e.g. during world creation).
	 * Checks the overworld generator directly regardless of cached state or config mode.
	 */
	public static boolean isWaterworldLevel(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) return false;
		String mode = WaterworldConfig.INSTANCE.activationMode.trim().toLowerCase();
		if ("always".equals(mode)) return true;
		if ("never".equals(mode)) return false;
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		return generator.getBiomeSource() instanceof WaterworldBiomeSource;
	}

	private static boolean detectWaterworldOverworld(MinecraftServer server) {
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) return false;

		ChunkGenerator generator = overworld.getChunkSource().getGenerator();
		return generator.getBiomeSource() instanceof WaterworldBiomeSource;
	}
}
