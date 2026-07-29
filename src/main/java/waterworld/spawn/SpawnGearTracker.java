package waterworld.spawn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import waterworld.WaterworldMod;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-world record of players who already received Waterworld spawn gear.
 * Prefer this over PLAY_TIME alone (spectator holds tick play time).
 */
public final class SpawnGearTracker {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
	private static final String FILE_NAME = "waterworld_spawn_gear.json";

	private static final Set<UUID> GIVEN = ConcurrentHashMap.newKeySet();
	private static Path savePath;

	private SpawnGearTracker() {
	}

	public static void onServerStarted(MinecraftServer server) {
		GIVEN.clear();
		savePath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
		load();
	}

	public static void onServerStopped(MinecraftServer server) {
		save();
		GIVEN.clear();
		savePath = null;
	}

	public static boolean hasReceived(UUID playerId) {
		return GIVEN.contains(playerId);
	}

	public static void markReceived(UUID playerId) {
		if (GIVEN.add(playerId)) {
			save();
		}
	}

	private static void load() {
		if (savePath == null || !Files.exists(savePath)) return;
		try {
			String json = Files.readString(savePath);
			Set<String> ids = GSON.fromJson(json, SET_TYPE);
			if (ids == null) return;
			for (String id : ids) {
				try {
					GIVEN.add(UUID.fromString(id));
				} catch (IllegalArgumentException ignored) {
				}
			}
			WaterworldMod.LOGGER.info("Loaded {} spawn-gear recipients", GIVEN.size());
		} catch (IOException e) {
			WaterworldMod.LOGGER.warn("Failed to read {}", savePath.getFileName(), e);
		}
	}

	private static void save() {
		if (savePath == null) return;
		try {
			Files.createDirectories(savePath.getParent());
			Set<String> ids = new HashSet<>();
			for (UUID id : GIVEN) {
				ids.add(id.toString());
			}
			Files.writeString(savePath, GSON.toJson(ids));
		} catch (IOException e) {
			WaterworldMod.LOGGER.warn("Failed to write {}", savePath.getFileName(), e);
		}
	}
}
