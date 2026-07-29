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
 * Per-world record of party IDs that already received a spawn island
 * (one island per party, not per player).
 */
public final class PartyIslandTracker {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
	private static final String FILE_NAME = "waterworld_party_islands.json";

	private static final Set<UUID> GENERATED = ConcurrentHashMap.newKeySet();
	private static Path savePath;

	private PartyIslandTracker() {
	}

	public static void onServerStarted(MinecraftServer server) {
		GENERATED.clear();
		savePath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
		load();
	}

	public static void onServerStopped(MinecraftServer server) {
		save();
		GENERATED.clear();
		savePath = null;
	}

	public static boolean hasIsland(UUID partyId) {
		return GENERATED.contains(partyId);
	}

	public static void markIsland(UUID partyId) {
		if (GENERATED.add(partyId)) {
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
					GENERATED.add(UUID.fromString(id));
				} catch (IllegalArgumentException ignored) {
				}
			}
			WaterworldMod.LOGGER.info("Loaded {} party spawn islands", GENERATED.size());
		} catch (IOException e) {
			WaterworldMod.LOGGER.warn("Failed to read {}", savePath.getFileName(), e);
		}
	}

	private static void save() {
		if (savePath == null) return;
		try {
			Files.createDirectories(savePath.getParent());
			Set<String> ids = new HashSet<>();
			for (UUID id : GENERATED) {
				ids.add(id.toString());
			}
			Files.writeString(savePath, GSON.toJson(ids));
		} catch (IOException e) {
			WaterworldMod.LOGGER.warn("Failed to write {}", savePath.getFileName(), e);
		}
	}
}
