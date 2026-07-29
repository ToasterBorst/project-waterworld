package waterworld;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import waterworld.worldgen.WaterworldBiomeSource;

/**
 * Detects whether the current server is running a Waterworld world type
 * and gates mod gameplay effects accordingly.
 *
 * Detection is cached per server lifecycle to avoid repeated generator
 * lookups on every mob tick.
 *
 * <p>Worldgen mixins must use {@link #usesWaterworldBiomeSource} — never the
 * gameplay cache — because chunk generation can run before {@code SERVER_STARTED}
 * and must not follow {@code activation_mode}.
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
		String mode = WaterworldConfig.normalizeActivationMode(WaterworldConfig.INSTANCE.activationMode);

		activeForCurrentServer = switch (mode) {
			case "always" -> true;
			case "never" -> false;
			default -> detectWaterworldOverworld(server);
		};

		WaterworldMod.LOGGER.info("Waterworld activation: mode={}, active={}",
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
	 * True when the chunk generator uses our biome source. For worldgen mixins only —
	 * ignores {@code activation_mode} and the gameplay cache.
	 */
	public static boolean usesWaterworldBiomeSource(ChunkGenerator generator) {
		return generator != null && generator.getBiomeSource() instanceof WaterworldBiomeSource;
	}

	/**
	 * Resolves the overworld generator from a worldgen level accessor when possible.
	 */
	public static boolean usesWaterworldBiomeSource(LevelAccessor level) {
		if (level instanceof WorldGenLevel worldGenLevel) {
			return usesWaterworldBiomeSource(worldGenLevel.getLevel().getChunkSource().getGenerator());
		}
		if (level instanceof ServerLevel serverLevel) {
			return usesWaterworldBiomeSource(serverLevel.getChunkSource().getGenerator());
		}
		return false;
	}

	/**
	 * Direct generator check for use before SERVER_STARTED (e.g. during world creation).
	 * Respects {@code activation_mode} for spawn / gameplay-adjacent early hooks.
	 */
	public static boolean isWaterworldLevel(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) return false;
		String mode = WaterworldConfig.normalizeActivationMode(WaterworldConfig.INSTANCE.activationMode);
		if ("always".equals(mode)) return true;
		if ("never".equals(mode)) return false;
		return usesWaterworldBiomeSource(level.getChunkSource().getGenerator());
	}

	private static boolean detectWaterworldOverworld(MinecraftServer server) {
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) return false;
		return usesWaterworldBiomeSource(overworld.getChunkSource().getGenerator());
	}
}
