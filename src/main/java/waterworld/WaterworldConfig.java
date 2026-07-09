package waterworld;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class WaterworldConfig {

	private static final String FILE_NAME = "project-waterworld.properties";
	private static final String LEGACY_FILE_NAME = "project-waterworld.json";

	public static WaterworldConfig INSTANCE = new WaterworldConfig();

	// --- Features ---
	public String activationMode = "auto";
	public boolean bambooReplacesSticks = true;
	public boolean wildGuardianSpawns = true;
	public boolean drownedRideGuardians = true;
	public boolean drownedCanGoOnLand = true;
	public boolean pillagerArmor = true;
	public boolean mobsCanExitBoats = true;
	public boolean mobsCanPilotBoats = true;
	public boolean wanderingTraderBoats = true;
	public boolean oceanPillagerPatrols = true;
	public boolean turtleOceanSpawns = true;

	// --- Spawn Options ---
	public String spawnOceanBiome = "";
	public boolean spawnIsland = false;
	public boolean spawnGear = true;

	// --- Tunable Values ---
	public int guardianSpawnWeight = 1;
	public int turtleSpawnWeight = 5;
	public double guardianSpawnChance = 0.04;
	public double drownedRiderChance = 0.12;
	public double tridentRiderChance = 0.20;
	public double pillagerArmorChance = 0.3;
	public boolean armorScalesWithDifficulty = true;

	// --- Difficulty Scaling (days = 24000 ticks) ---
	public int tridentDrownedMinDays = 10;
	public int guardianMinDays = 3;
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
		if (currentDay >= fullStrengthDays) return 1.0;
		return (double) (currentDay - minDays) / (fullStrengthDays - minDays);
	}

	public static WaterworldConfig load(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		Path legacyFile = configDir.resolve(LEGACY_FILE_NAME);

		if (!Files.exists(file) && Files.exists(legacyFile)) {
			WaterworldConfig migrated = loadLegacyJson(legacyFile);
			if (migrated != null) {
				migrated.save(configDir);
				try {
					Files.delete(legacyFile);
				} catch (IOException ignored) {
				}
				ProjectWaterworld.LOGGER.info("Migrated config from {} to {}", LEGACY_FILE_NAME, FILE_NAME);
				return migrated;
			}
		}

		if (Files.exists(file)) {
			try {
				Properties props = new Properties();
				try (var reader = Files.newBufferedReader(file)) {
					props.load(reader);
				}
				WaterworldConfig config = new WaterworldConfig();
				config.readProperties(props);
				config.save(configDir);
				return config;
			} catch (IOException e) {
				ProjectWaterworld.LOGGER.error("Failed to read config, using defaults", e);
			}
		}

		WaterworldConfig config = new WaterworldConfig();
		config.save(configDir);
		return config;
	}

	private static WaterworldConfig loadLegacyJson(Path file) {
		try {
			String json = Files.readString(file);
			Gson gson = new Gson();
			LegacyJsonConfig legacy = gson.fromJson(json, LegacyJsonConfig.class);
			if (legacy == null) return null;

			WaterworldConfig config = new WaterworldConfig();
			config.activationMode = legacy.activation_mode != null ? legacy.activation_mode : "auto";
			config.bambooReplacesSticks = legacy.bamboo_replaces_sticks;
			config.wildGuardianSpawns = legacy.wild_guardian_spawns;
			config.drownedRideGuardians = legacy.drowned_ride_guardians;
			config.drownedCanGoOnLand = legacy.drowned_can_go_on_land;
			config.pillagerArmor = legacy.pillager_armor;
			config.mobsCanExitBoats = legacy.mobs_can_exit_boats;
			config.mobsCanPilotBoats = legacy.mobs_can_pilot_boats;
			config.wanderingTraderBoats = legacy.wandering_trader_boats;
			config.oceanPillagerPatrols = legacy.ocean_pillager_patrols;
			config.turtleOceanSpawns = legacy.turtle_ocean_spawns;
			config.spawnOceanBiome = legacy.spawn_ocean_biome != null ? legacy.spawn_ocean_biome : "";
			config.spawnIsland = legacy.spawn_island;
			config.spawnGear = legacy.spawn_gear;
			config.guardianSpawnWeight = legacy.guardian_spawn_weight;
			config.turtleSpawnWeight = legacy.turtle_spawn_weight;
			config.guardianSpawnChance = legacy.guardian_spawn_chance;
			config.drownedRiderChance = legacy.drowned_rider_chance;
			config.tridentRiderChance = legacy.trident_rider_chance;
			config.pillagerArmorChance = legacy.pillager_armor_chance;
			config.armorScalesWithDifficulty = legacy.armor_scales_with_difficulty;
			config.tridentDrownedMinDays = legacy.trident_drowned_min_days;
			return config;
		} catch (Exception e) {
			ProjectWaterworld.LOGGER.warn("Failed to migrate legacy JSON config", e);
			return null;
		}
	}

	private void readProperties(Properties props) {
		activationMode = getString(props, "activation_mode", activationMode);
		bambooReplacesSticks = getBool(props, "bamboo_replaces_sticks", bambooReplacesSticks);
		wildGuardianSpawns = getBool(props, "wild_guardian_spawns", wildGuardianSpawns);
		drownedRideGuardians = getBool(props, "drowned_ride_guardians", drownedRideGuardians);
		drownedCanGoOnLand = getBool(props, "drowned_can_go_on_land", drownedCanGoOnLand);
		pillagerArmor = getBool(props, "pillager_armor", pillagerArmor);
		mobsCanExitBoats = getBool(props, "mobs_can_exit_boats", mobsCanExitBoats);
		mobsCanPilotBoats = getBool(props, "mobs_can_pilot_boats", mobsCanPilotBoats);
		wanderingTraderBoats = getBool(props, "wandering_trader_boats", wanderingTraderBoats);
		oceanPillagerPatrols = getBool(props, "ocean_pillager_patrols", oceanPillagerPatrols);
		turtleOceanSpawns = getBool(props, "turtle_ocean_spawns", turtleOceanSpawns);

		spawnOceanBiome = getString(props, "spawn_ocean_biome", spawnOceanBiome);
		spawnIsland = getBool(props, "spawn_island", spawnIsland);
		spawnGear = getBool(props, "spawn_gear", spawnGear);

		guardianSpawnWeight = getInt(props, "guardian_spawn_weight", guardianSpawnWeight);
		turtleSpawnWeight = getInt(props, "turtle_spawn_weight", turtleSpawnWeight);
		guardianSpawnChance = getDouble(props, "guardian_spawn_chance", guardianSpawnChance);
		drownedRiderChance = getDouble(props, "drowned_rider_chance", drownedRiderChance);
		tridentRiderChance = getDouble(props, "trident_rider_chance", tridentRiderChance);
		pillagerArmorChance = getDouble(props, "pillager_armor_chance", pillagerArmorChance);
		armorScalesWithDifficulty = getBool(props, "armor_scales_with_difficulty", armorScalesWithDifficulty);

		tridentDrownedMinDays = getInt(props, "trident_drowned_min_days", tridentDrownedMinDays);
		guardianMinDays = getInt(props, "guardian_min_days", guardianMinDays);
		guardianFullStrengthDays = getInt(props, "guardian_full_strength_days", guardianFullStrengthDays);
		patrolMinDays = getInt(props, "patrol_min_days", patrolMinDays);
		patrolFullStrengthDays = getInt(props, "patrol_full_strength_days", patrolFullStrengthDays);
		wanderingTraderMinDays = getInt(props, "wandering_trader_min_days", wanderingTraderMinDays);
		wanderingTraderFullStrengthDays = getInt(props, "wandering_trader_full_strength_days", wanderingTraderFullStrengthDays);
		drownedRiderMinDays = getInt(props, "drowned_rider_min_days", drownedRiderMinDays);
		drownedRiderFullStrengthDays = getInt(props, "drowned_rider_full_strength_days", drownedRiderFullStrengthDays);
	}

	public void save(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		try {
			Files.createDirectories(configDir);

			String content = """
					# Project Waterworld Configuration
					#
					# Changes take effect on next world load unless otherwise noted.
					# Land structures (villages, outposts, igloos, pyramids, temples, huts,
					# mansions, trail ruins) are disabled by the built-in datapack.

					# --- Activation ---

					# Controls when mod gameplay effects (spawns, boat AI, etc.) are active.
					# auto   = only active in Waterworld world type (recommended)
					# always = active in all world types
					# never  = disabled (worldgen-only, no gameplay changes)
					activation_mode=%s

					# --- Features ---

					# Bamboo can substitute for sticks in any crafting recipe
					bamboo_replaces_sticks=%s

					# Wild guardian spawns in ocean biomes (outside monuments)
					wild_guardian_spawns=%s

					# Drowned riders on wild guardians, with trident chance
					drowned_ride_guardians=%s

					# Drowned can roam on land instead of returning to water
					drowned_can_go_on_land=%s

					# Illagers spawn with armor during patrols and raids
					pillager_armor=%s

					# Intelligent mobs can exit boats when they reach land
					mobs_can_exit_boats=%s

					# Illagers and wandering traders can steer boats
					mobs_can_pilot_boats=%s

					# Wandering traders spawn in boats at sea with one llama
					wandering_trader_boats=%s

					# Pillager patrols and raids spawn in boats on water
					ocean_pillager_patrols=%s

					# Turtles spawn naturally in warm and lukewarm oceans
					turtle_ocean_spawns=%s

					# --- Spawn Options ---

					# Force spawn in a specific ocean biome (empty = disabled)
					# Examples: warm_ocean, lukewarm_ocean, deep_ocean
					spawn_ocean_biome=%s

					# Generate a small island at world spawn
					spawn_island=%s

					# Give players a bamboo chest raft with starter items on first spawn
					spawn_gear=%s

					# --- Tunable Values ---

					# Spawn weight for wild guardians (squid is ~5, keep very low)
					guardian_spawn_weight=%d

					# Spawn weight for ocean turtles
					turtle_spawn_weight=%d

					# Chance a guardian spawn attempt succeeds (0.0-1.0, lower = rarer)
					guardian_spawn_chance=%s

					# Chance a wild guardian spawns with a drowned rider (0.0-1.0)
					drowned_rider_chance=%s

					# Chance a drowned rider carries a trident (0.0-1.0)
					trident_rider_chance=%s

					# Base chance per armor piece for illagers (0.0-1.0)
					pillager_armor_chance=%s

					# Whether armor tier scales with world difficulty setting
					armor_scales_with_difficulty=%s

					# --- Difficulty Scaling ---
					# Controls when hostile spawns activate and ramp up over in-game days.
					# Each day is 24000 ticks (~20 real minutes).
					# Before min_days the system is inactive; by full_strength_days it
					# reaches the configured chance values above. Linear ramp between.

					# Days before drowned can spawn with tridents (0 = immediate)
					trident_drowned_min_days=%d

					# Days before wild guardians start spawning
					guardian_min_days=%d

					# Day guardian spawn chance reaches full configured value
					guardian_full_strength_days=%d

					# Days before pillager patrols begin
					patrol_min_days=%d

					# Day patrol frequency reaches full rate
					patrol_full_strength_days=%d

					# Days before wandering traders appear
					wandering_trader_min_days=%d

					# Day wandering trader frequency reaches full rate
					wandering_trader_full_strength_days=%d

					# Days before drowned riders appear on guardians
					drowned_rider_min_days=%d

					# Day drowned rider chance reaches full configured value
					drowned_rider_full_strength_days=%d
					""".formatted(
					activationMode,
					bambooReplacesSticks,
					wildGuardianSpawns,
					drownedRideGuardians,
					drownedCanGoOnLand,
					pillagerArmor,
					mobsCanExitBoats,
					mobsCanPilotBoats,
					wanderingTraderBoats,
					oceanPillagerPatrols,
					turtleOceanSpawns,
					spawnOceanBiome,
					spawnIsland,
					spawnGear,
					guardianSpawnWeight,
					turtleSpawnWeight,
					formatDouble(guardianSpawnChance),
					formatDouble(drownedRiderChance),
					formatDouble(tridentRiderChance),
					formatDouble(pillagerArmorChance),
					armorScalesWithDifficulty,
					tridentDrownedMinDays,
					guardianMinDays,
					guardianFullStrengthDays,
					patrolMinDays,
					patrolFullStrengthDays,
					wanderingTraderMinDays,
					wanderingTraderFullStrengthDays,
					drownedRiderMinDays,
					drownedRiderFullStrengthDays);

			Files.writeString(file, content);
		} catch (IOException e) {
			ProjectWaterworld.LOGGER.error("Failed to save config", e);
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
		String s = String.valueOf(value);
		if (s.endsWith("0") && s.contains(".") && s.indexOf('.') < s.length() - 2) {
			return s;
		}
		return s;
	}

	@SuppressWarnings("unused")
	private static class LegacyJsonConfig {
		String activation_mode = "auto";
		boolean bamboo_replaces_sticks = true;
		boolean wild_guardian_spawns = true;
		boolean drowned_ride_guardians = true;
		boolean drowned_can_go_on_land = true;
		boolean pillager_armor = true;
		boolean mobs_can_exit_boats = true;
		boolean mobs_can_pilot_boats = true;
		boolean wandering_trader_boats = true;
		boolean ocean_pillager_patrols = true;
		boolean turtle_ocean_spawns = true;
		String spawn_ocean_biome = "";
		boolean spawn_island = false;
		boolean spawn_gear = true;
		int guardian_spawn_weight = 1;
		int turtle_spawn_weight = 5;
		double guardian_spawn_chance = 0.05;
		double drowned_rider_chance = 0.15;
		double trident_rider_chance = 0.25;
		double pillager_armor_chance = 0.3;
		boolean armor_scales_with_difficulty = true;
		int trident_drowned_min_days = 10;
	}
}
