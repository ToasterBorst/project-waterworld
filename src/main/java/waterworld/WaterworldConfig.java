package waterworld;

import com.google.gson.Gson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class WaterworldConfig {

	private static final String FILE_NAME = "waterworld.properties";
	private static final String LEGACY_PROPERTIES_FILE_NAME = "project-waterworld.properties";
	private static final String LEGACY_JSON_FILE_NAME = "project-waterworld.json";
	private static final Set<String> ACTIVATION_MODES = Set.of("auto", "always", "never");

	public static WaterworldConfig INSTANCE = new WaterworldConfig();

	public String activationMode = "auto";
	public boolean wildGuardianSpawns = true;
	public boolean drownedRideGuardians = true;
	public boolean drownedCanGoOnLand = true;
	public boolean wanderingTraderBoats = true;
	public boolean oceanPillagerPatrols = true;
	public boolean turtleOceanSpawns = true;

	public boolean spawnIsland = false;
	public boolean spawnGear = true;
	/** Comma-separated item ids for the spawn raft chest; optional {@code :count}. */
	public String spawnGearItems = "minecraft:bamboo,minecraft:fishing_rod";

	/**
	 * Ocean waterline for structures/biomes. Keep in sync with noise_settings
	 * {@code sea_level} for new worlds (default {@link WaterworldConstants#DEFAULT_SEA_LEVEL}).
	 */
	public int seaLevel = WaterworldConstants.DEFAULT_SEA_LEVEL;

	public int guardianSpawnWeight = 1;
	public int turtleSpawnWeight = 5;
	public double guardianSpawnChance = 0.01;
	/** Chance a wild guardian (outside monuments) spawns with a baby drowned rider. */
	public double drownedRiderChance = 0.05;
	/**
	 * Multiplies guardian {@code MOVEMENT_SPEED} while a drowned is riding
	 * ({@code 1.0} = off). Default {@code 0.55} ≈ 0.275 effective speed so boats can escape.
	 */
	public double mountedGuardianSpeedFactor = 0.55;
	/**
	 * Vanilla {@code spawn_costs} charge for drowned in ocean biomes (0 = disabled).
	 * Limits local packing via {@code SpawnState.canSpawn}; restart required.
	 * Vanilla-style pair (e.g. soul sand valley) is charge 0.15 / budget 0.7.
	 */
	public double drownedSpawnCharge = 0.15;
	/** Energy budget paired with {@link #drownedSpawnCharge} (vanilla spawn_costs). */
	public double drownedSpawnEnergyBudget = 0.7;
	public int tridentDrownedMinDays = 10;
	public int guardianMinDays = 5;
	public int guardianFullStrengthDays = 20;
	public int patrolMinDays = 5;
	public int patrolFullStrengthDays = 24;
	public int wanderingTraderMinDays = 2;
	public int wanderingTraderFullStrengthDays = 12;
	public int drownedRiderMinDays = 5;
	public int drownedRiderFullStrengthDays = 18;

	/**
	 * Returns a scaling factor between 0.0 and 1.0 based on world age.
	 * Before minDays the factor is 0; at or after fullStrengthDays it is 1.
	 */
	public static double dayScaleFactor(long gameTime, int minDays, int fullStrengthDays) {
		long currentDay = gameTime / 24000L;
		if (currentDay < minDays) return 0.0;
		int span = Math.max(1, fullStrengthDays - minDays);
		if (currentDay >= minDays + span) return 1.0;
		return (double) (currentDay - minDays) / span;
	}

	public static String normalizeActivationMode(String mode) {
		if (mode == null) return "auto";
		String normalized = mode.trim().toLowerCase();
		return ACTIVATION_MODES.contains(normalized) ? normalized : "auto";
	}

	public static WaterworldConfig load(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		Path legacyProperties = configDir.resolve(LEGACY_PROPERTIES_FILE_NAME);
		Path legacyJson = configDir.resolve(LEGACY_JSON_FILE_NAME);

		if (!Files.exists(file) && Files.exists(legacyProperties)) {
			WaterworldConfig migrated = loadPropertiesFile(legacyProperties);
			if (migrated != null) {
				migrated.save(configDir);
				tryDelete(legacyProperties);
				WaterworldMod.LOGGER.info("Migrated config from {} to {}", LEGACY_PROPERTIES_FILE_NAME, FILE_NAME);
				return migrated;
			}
		}

		if (!Files.exists(file) && Files.exists(legacyJson)) {
			WaterworldConfig migrated = loadLegacyJson(legacyJson);
			if (migrated != null) {
				migrated.save(configDir);
				tryDelete(legacyJson);
				WaterworldMod.LOGGER.info("Migrated config from {} to {}", LEGACY_JSON_FILE_NAME, FILE_NAME);
				return migrated;
			}
		}

		if (Files.exists(file)) {
			WaterworldConfig config = loadPropertiesFile(file);
			if (config != null) {
				return config;
			}
		}

		WaterworldConfig config = new WaterworldConfig();
		config.save(configDir);
		return config;
	}

	private static WaterworldConfig loadPropertiesFile(Path file) {
		try {
			Properties props = new Properties();
			try (var reader = Files.newBufferedReader(file)) {
				props.load(reader);
			}
			WaterworldConfig config = new WaterworldConfig();
			config.readProperties(props);
			return config;
		} catch (IOException e) {
			WaterworldMod.LOGGER.error("Failed to read config {}, using defaults", file.getFileName(), e);
			return null;
		}
	}

	private static WaterworldConfig loadLegacyJson(Path file) {
		try {
			String json = Files.readString(file);
			Gson gson = new Gson();
			LegacyJsonConfig legacy = gson.fromJson(json, LegacyJsonConfig.class);
			if (legacy == null) return null;

			WaterworldConfig config = new WaterworldConfig();
			config.activationMode = normalizeActivationMode(legacy.activation_mode);
			config.wildGuardianSpawns = legacy.wild_guardian_spawns;
			config.drownedRideGuardians = legacy.drowned_ride_guardians;
			config.drownedCanGoOnLand = legacy.drowned_can_go_on_land;
			config.wanderingTraderBoats = legacy.wandering_trader_boats;
			config.oceanPillagerPatrols = legacy.ocean_pillager_patrols;
			config.turtleOceanSpawns = legacy.turtle_ocean_spawns;
			config.spawnIsland = legacy.spawn_island;
			config.spawnGear = legacy.spawn_gear;
			config.guardianSpawnWeight = legacy.guardian_spawn_weight;
			config.turtleSpawnWeight = legacy.turtle_spawn_weight;
			config.guardianSpawnChance = legacy.guardian_spawn_chance;
			config.drownedRiderChance = legacy.drowned_rider_chance;
			config.tridentDrownedMinDays = legacy.trident_drowned_min_days;
			config.guardianMinDays = legacy.guardian_min_days;
			config.guardianFullStrengthDays = legacy.guardian_full_strength_days;
			config.patrolMinDays = legacy.patrol_min_days;
			config.patrolFullStrengthDays = legacy.patrol_full_strength_days;
			config.wanderingTraderMinDays = legacy.wandering_trader_min_days;
			config.wanderingTraderFullStrengthDays = legacy.wandering_trader_full_strength_days;
			config.drownedRiderMinDays = legacy.drowned_rider_min_days;
			config.drownedRiderFullStrengthDays = legacy.drowned_rider_full_strength_days;
			config.sanitize();
			return config;
		} catch (Exception e) {
			WaterworldMod.LOGGER.warn("Failed to migrate legacy JSON config", e);
			return null;
		}
	}

	private void readProperties(Properties props) {
		activationMode = normalizeActivationMode(getString(props, "activation_mode", activationMode));
		wildGuardianSpawns = getBool(props, "wild_guardian_spawns", wildGuardianSpawns);
		drownedRideGuardians = getBool(props, "drowned_ride_guardians", drownedRideGuardians);
		drownedCanGoOnLand = getBool(props, "drowned_can_go_on_land", drownedCanGoOnLand);
		wanderingTraderBoats = getBool(props, "wandering_trader_boats", wanderingTraderBoats);
		oceanPillagerPatrols = getBool(props, "ocean_pillager_patrols", oceanPillagerPatrols);
		turtleOceanSpawns = getBool(props, "turtle_ocean_spawns", turtleOceanSpawns);

		spawnIsland = getBool(props, "spawn_island", spawnIsland);
		spawnGear = getBool(props, "spawn_gear", spawnGear);
		spawnGearItems = getString(props, "spawn_gear_items", spawnGearItems);
		seaLevel = getInt(props, "sea_level", seaLevel);

		guardianSpawnWeight = getInt(props, "guardian_spawn_weight", guardianSpawnWeight);
		turtleSpawnWeight = getInt(props, "turtle_spawn_weight", turtleSpawnWeight);
		guardianSpawnChance = getDouble(props, "guardian_spawn_chance", guardianSpawnChance);
		drownedRiderChance = getDouble(props, "drowned_rider_chance", drownedRiderChance);
		mountedGuardianSpeedFactor = getDouble(props, "mounted_guardian_speed_factor", mountedGuardianSpeedFactor);
		drownedSpawnCharge = getDouble(props, "drowned_spawn_charge", drownedSpawnCharge);
		drownedSpawnEnergyBudget = getDouble(props, "drowned_spawn_energy_budget", drownedSpawnEnergyBudget);
		// A prior release shipped these inverted (charge 1.0 / budget 0.15), which
		// suppressed nearly all ocean drowned and flooded oceans with guardians.
		if (drownedSpawnCharge == 1.0 && drownedSpawnEnergyBudget == 0.15) {
			drownedSpawnCharge = 0.15;
			drownedSpawnEnergyBudget = 0.7;
		}
		tridentDrownedMinDays = getInt(props, "trident_drowned_min_days", tridentDrownedMinDays);
		guardianMinDays = getInt(props, "guardian_min_days", guardianMinDays);
		guardianFullStrengthDays = getInt(props, "guardian_full_strength_days", guardianFullStrengthDays);
		patrolMinDays = getInt(props, "patrol_min_days", patrolMinDays);
		patrolFullStrengthDays = getInt(props, "patrol_full_strength_days", patrolFullStrengthDays);
		wanderingTraderMinDays = getInt(props, "wandering_trader_min_days", wanderingTraderMinDays);
		wanderingTraderFullStrengthDays = getInt(props, "wandering_trader_full_strength_days", wanderingTraderFullStrengthDays);
		drownedRiderMinDays = getInt(props, "drowned_rider_min_days", drownedRiderMinDays);
		drownedRiderFullStrengthDays = getInt(props, "drowned_rider_full_strength_days", drownedRiderFullStrengthDays);
		sanitize();
	}

	/** Clamps chances and day ranges so runtime math stays safe. */
	public void sanitize() {
		activationMode = normalizeActivationMode(activationMode);
		if (spawnGearItems == null) spawnGearItems = "";

		seaLevel = Math.max(32, Math.min(200, seaLevel));
		guardianSpawnWeight = Math.max(0, guardianSpawnWeight);
		turtleSpawnWeight = Math.max(0, turtleSpawnWeight);
		guardianSpawnChance = clamp01(guardianSpawnChance);
		drownedRiderChance = clamp01(drownedRiderChance);
		mountedGuardianSpeedFactor = Math.max(0.1, Math.min(1.0, mountedGuardianSpeedFactor));
		drownedSpawnCharge = Math.max(0.0, drownedSpawnCharge);
		drownedSpawnEnergyBudget = Math.max(0.0, drownedSpawnEnergyBudget);

		tridentDrownedMinDays = Math.max(0, tridentDrownedMinDays);
		guardianMinDays = Math.max(0, guardianMinDays);
		guardianFullStrengthDays = Math.max(guardianMinDays + 1, guardianFullStrengthDays);
		patrolMinDays = Math.max(0, patrolMinDays);
		patrolFullStrengthDays = Math.max(patrolMinDays + 1, patrolFullStrengthDays);
		wanderingTraderMinDays = Math.max(0, wanderingTraderMinDays);
		wanderingTraderFullStrengthDays = Math.max(wanderingTraderMinDays + 1, wanderingTraderFullStrengthDays);
		drownedRiderMinDays = Math.max(0, drownedRiderMinDays);
		drownedRiderFullStrengthDays = Math.max(drownedRiderMinDays + 1, drownedRiderFullStrengthDays);
	}

	private static double clamp01(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	/**
	 * Parses {@link #spawnGearItems} into stacks for the spawn raft chest.
	 * Invalid entries are skipped with a warning.
	 */
	public List<ItemStack> createSpawnGearStacks() {
		sanitize();
		List<ItemStack> stacks = new ArrayList<>();
		if (spawnGearItems.isBlank()) return stacks;

		for (String raw : spawnGearItems.split(",")) {
			String entry = raw.trim();
			if (entry.isEmpty()) continue;

			int count = 1;
			String idPart = entry;
			int lastColon = entry.lastIndexOf(':');
			int firstColon = entry.indexOf(':');
			// namespace:path:count — count only when there are at least two colons
			if (firstColon >= 0 && lastColon > firstColon) {
				String maybeCount = entry.substring(lastColon + 1).trim();
				try {
					count = Integer.parseInt(maybeCount);
					idPart = entry.substring(0, lastColon).trim();
				} catch (NumberFormatException ignored) {
					// Treat whole string as an id (unusual path containing digits after colon)
				}
			}

			if (count <= 0) continue;

			Identifier id;
			try {
				if (idPart.contains(":")) {
					id = Identifier.parse(idPart);
				} else {
					id = Identifier.fromNamespaceAndPath("minecraft", idPart);
				}
			} catch (RuntimeException e) {
				WaterworldMod.LOGGER.warn("Invalid spawn_gear_items entry '{}', skipping", entry);
				continue;
			}

			if (!BuiltInRegistries.ITEM.containsKey(id)) {
				WaterworldMod.LOGGER.warn("Unknown item '{}' in spawn_gear_items, skipping", id);
				continue;
			}
			Item item = BuiltInRegistries.ITEM.getValue(id);
			stacks.add(new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize())));
		}
		return stacks;
	}

	public void save(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		sanitize();
		try {
			Files.createDirectories(configDir);

			String content = """
					# Waterworld Configuration
					#
					# Live file: config/waterworld.properties
					# Most toggles apply on next world load. Spawn weights for guardians
					# and turtles require a full game restart (BiomeModifications).
					# Land structures (villages, igloos, mansions) are disabled by the
					# mod datapack for every overworld while this mod is installed.

					# --- Activation ---

					# Controls when mod gameplay effects (spawns, boat AI, etc.) are active.
					# auto   = only active in Waterworld world type (recommended)
					# always = active in all world types
					# never  = disabled (no gameplay changes; datapack structure overrides still apply)
					activation_mode=%s

					# --- Player Spawn ---

					# Generate a small island at world spawn
					spawn_island=%s

					# Give players a bamboo chest raft with starter items on first spawn
					spawn_gear=%s

					# Comma-separated item ids for the spawn raft chest; optional :count (default 1).
					# Example: minecraft:bamboo,minecraft:fishing_rod
					# Empty = raft with empty chest
					spawn_gear_items=%s

					# Ocean waterline for ships/biomes (default 101). For new worlds, keep
					# data/waterworld/worldgen/noise_settings/waterworld.json sea_level
					# equal to this value. Existing chunks are not re-flooded.
					sea_level=%d

					# --- Guardians & Drowned ---

					# Wild guardian spawns in ocean biomes (outside monuments)
					# Restart required after changing weight
					wild_guardian_spawns=%s

					# Spawn weight for wild guardians (squid is ~5, keep very low; restart required)
					guardian_spawn_weight=%d

					# Chance a guardian spawn attempt succeeds (0.0-1.0, lower = rarer)
					guardian_spawn_chance=%s

					# Days before wild guardians start spawning
					guardian_min_days=%d

					# Day guardian spawn chance reaches full configured value
					guardian_full_strength_days=%d

					# Drowned riders on wild guardians (baby drowned; no tridents)
					drowned_ride_guardians=%s

					# Chance a wild guardian (outside monuments) spawns with a drowned rider (0.0-1.0)
					drowned_rider_chance=%s

					# Multiplies guardian swim speed while a drowned is riding (0.1-1.0; 1.0 = off).
					# Bare guardians are unchanged. Default 0.55 lets boats escape continuous chase.
					mounted_guardian_speed_factor=%s

					# Days before drowned riders appear on guardians
					drowned_rider_min_days=%d

					# Day drowned rider chance reaches full configured value
					drowned_rider_full_strength_days=%d

					# Days before drowned can spawn with tridents (0 = immediate)
					trident_drowned_min_days=%d

					# Drowned can roam on land instead of returning to water
					drowned_can_go_on_land=%s

					# Vanilla spawn_costs for ocean drowned (restart required). charge=0 disables.
					# Limits local packing so oceans stay threatening without stacking endlessly.
					# Vanilla-style values: charge=0.15, energy_budget=0.7 (higher charge = sparser).
					drowned_spawn_charge=%s
					drowned_spawn_energy_budget=%s

					# --- Turtles ---

					# Turtles spawn naturally in biomes tagged #waterworld:turtle_spawns
					# Restart required after changing weight
					turtle_ocean_spawns=%s

					# Spawn weight for ocean turtles (restart required)
					turtle_spawn_weight=%d

					# --- Illagers & Patrols ---

					# Pillager patrols and raids spawn in boats on water
					ocean_pillager_patrols=%s

					# Days before pillager patrols begin
					patrol_min_days=%d

					# Day patrol frequency reaches full rate
					patrol_full_strength_days=%d

					# --- Wandering Traders ---

					# Wandering traders spawn in boats at sea with one llama
					wandering_trader_boats=%s

					# Days before wandering traders appear
					wandering_trader_min_days=%d

					# Day wandering trader frequency reaches full rate
					wandering_trader_full_strength_days=%d
					""".formatted(
					activationMode,
					spawnIsland,
					spawnGear,
					spawnGearItems,
					seaLevel,
					wildGuardianSpawns,
					guardianSpawnWeight,
					formatDouble(guardianSpawnChance),
					guardianMinDays,
					guardianFullStrengthDays,
					drownedRideGuardians,
					formatDouble(drownedRiderChance),
					formatDouble(mountedGuardianSpeedFactor),
					drownedRiderMinDays,
					drownedRiderFullStrengthDays,
					tridentDrownedMinDays,
					drownedCanGoOnLand,
					formatDouble(drownedSpawnCharge),
					formatDouble(drownedSpawnEnergyBudget),
					turtleOceanSpawns,
					turtleSpawnWeight,
					oceanPillagerPatrols,
					patrolMinDays,
					patrolFullStrengthDays,
					wanderingTraderBoats,
					wanderingTraderMinDays,
					wanderingTraderFullStrengthDays);

			Files.writeString(file, content);
		} catch (IOException e) {
			WaterworldMod.LOGGER.error("Failed to save config", e);
		}
	}

	private static void tryDelete(Path file) {
		try {
			Files.delete(file);
		} catch (IOException ignored) {
		}
	}

	private static boolean getBool(Properties props, String key, boolean defaultValue) {
		if (props.containsKey(key)) {
			return Boolean.parseBoolean(props.getProperty(key).trim());
		}
		return defaultValue;
	}

	private static int getInt(Properties props, String key, int defaultValue) {
		if (props.containsKey(key)) {
			try {
				return Integer.parseInt(props.getProperty(key).trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return defaultValue;
	}

	private static double getDouble(Properties props, String key, double defaultValue) {
		if (props.containsKey(key)) {
			try {
				return Double.parseDouble(props.getProperty(key).trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return defaultValue;
	}

	private static String getString(Properties props, String key, String defaultValue) {
		if (props.containsKey(key)) {
			return props.getProperty(key).trim();
		}
		return defaultValue;
	}

	private static String formatDouble(double value) {
		if (value == (long) value) {
			return String.valueOf((long) value);
		}
		return String.valueOf(value);
	}

	@SuppressWarnings("unused")
	private static class LegacyJsonConfig {
		String activation_mode = "auto";
		boolean wild_guardian_spawns = true;
		boolean drowned_ride_guardians = true;
		boolean drowned_can_go_on_land = true;
		boolean wandering_trader_boats = true;
		boolean ocean_pillager_patrols = true;
		boolean turtle_ocean_spawns = true;
		boolean spawn_island = false;
		boolean spawn_gear = true;
		int guardian_spawn_weight = 1;
		int turtle_spawn_weight = 5;
		double guardian_spawn_chance = 0.04;
		double drowned_rider_chance = 0.12;
		int trident_drowned_min_days = 10;
		int guardian_min_days = 3;
		int guardian_full_strength_days = 20;
		int patrol_min_days = 5;
		int patrol_full_strength_days = 24;
		int wandering_trader_min_days = 2;
		int wandering_trader_full_strength_days = 12;
		int drowned_rider_min_days = 5;
		int drowned_rider_full_strength_days = 18;
	}
}
