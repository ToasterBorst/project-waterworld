package waterworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class WaterworldConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.disableHtmlEscaping()
			.create();

	public static WaterworldConfig INSTANCE = new WaterworldConfig();

	@SerializedName("// Bamboo can substitute for sticks in any crafting recipe")
	public final String _comment_bamboo = "(default: true)";
	@SerializedName("bamboo_replaces_sticks")
	public boolean bambooReplacesSticks = true;

	@SerializedName("// Wild guardian spawns in ocean biomes")
	public final String _comment_guardians = "(default: true)";
	@SerializedName("wild_guardian_spawns")
	public boolean wildGuardianSpawns = true;

	@SerializedName("// Drowned riders on wild guardians, with trident chance")
	public final String _comment_riders = "(default: true)";
	@SerializedName("drowned_ride_guardians")
	public boolean drownedRideGuardians = true;

	@SerializedName("// Drowned can roam on land instead of returning to water")
	public final String _comment_land = "(default: true)";
	@SerializedName("drowned_can_go_on_land")
	public boolean drownedCanGoOnLand = true;

	@SerializedName("// Illagers spawn with armor during patrols and raids")
	public final String _comment_armor = "(default: true)";
	@SerializedName("pillager_armor")
	public boolean pillagerArmor = true;

	@SerializedName("// Intelligent mobs can exit boats when on land")
	public final String _comment_exit = "(default: true)";
	@SerializedName("mobs_can_exit_boats")
	public boolean mobsCanExitBoats = true;

	@SerializedName("// Illagers and wandering traders can steer boats")
	public final String _comment_pilot = "(default: true)";
	@SerializedName("mobs_can_pilot_boats")
	public boolean mobsCanPilotBoats = true;

	@SerializedName("// Wandering traders spawn in boats at sea with one llama")
	public final String _comment_trader = "(default: true)";
	@SerializedName("wandering_trader_boats")
	public boolean wanderingTraderBoats = true;

	@SerializedName("// Pillager patrols and raids spawn in boats on water")
	public final String _comment_patrols = "(default: true)";
	@SerializedName("ocean_pillager_patrols")
	public boolean oceanPillagerPatrols = true;

	@SerializedName("// Turtles spawn naturally in warm and lukewarm oceans")
	public final String _comment_turtles = "(default: true)";
	@SerializedName("turtle_ocean_spawns")
	public boolean turtleOceanSpawns = true;

	@SerializedName("// Land structures (villages, outposts, igloos, pyramids, temples, huts, mansions, trail ruins) are disabled by the built-in datapack and are not configurable here")
	public final String _comment_structures = "";

	@SerializedName("// --- Spawn options ---")
	public final String _comment_spawn_options = "";

	@SerializedName("// Force spawn in a specific ocean biome (empty = disabled)")
	public final String _comment_spawnBiome = "(default: \"\", e.g. \"warm_ocean\")";
	@SerializedName("spawn_ocean_biome")
	public String spawnOceanBiome = "";

	@SerializedName("// Generate a small island at world spawn")
	public final String _comment_spawnIsland = "(default: false)";
	@SerializedName("spawn_island")
	public boolean spawnIsland = false;

	@SerializedName("// Give players a bamboo chest raft with starter items on first spawn")
	public final String _comment_spawnGear = "(default: true)";
	@SerializedName("spawn_gear")
	public boolean spawnGear = true;

	@SerializedName("// --- Tunable values ---")
	public final String _comment_tunable = "";

	@SerializedName("// Spawn weight for wild guardians (squid is ~5, keep very low)")
	public final String _comment_gw = "(default: 1)";
	@SerializedName("guardian_spawn_weight")
	public int guardianSpawnWeight = 1;

	@SerializedName("// Spawn weight for ocean turtles")
	public final String _comment_tw = "(default: 5)";
	@SerializedName("turtle_spawn_weight")
	public int turtleSpawnWeight = 5;

	@SerializedName("// Chance a spawn attempt succeeds (0.0-1.0, lower = rarer guardians)")
	public final String _comment_gsc = "(default: 0.05)";
	@SerializedName("guardian_spawn_chance")
	public double guardianSpawnChance = 0.05;

	@SerializedName("// Chance a wild guardian spawns with a drowned rider (0.0-1.0)")
	public final String _comment_drc = "(default: 0.15)";
	@SerializedName("drowned_rider_chance")
	public double drownedRiderChance = 0.15;

	@SerializedName("// Chance a drowned rider carries a trident (0.0-1.0)")
	public final String _comment_trc = "(default: 0.25)";
	@SerializedName("trident_rider_chance")
	public double tridentRiderChance = 0.25;

	@SerializedName("// Base chance per armor piece for illagers (0.0-1.0)")
	public final String _comment_pac = "(default: 0.3)";
	@SerializedName("pillager_armor_chance")
	public double pillagerArmorChance = 0.3;

	@SerializedName("// Whether armor tier scales with world difficulty")
	public final String _comment_asd = "(default: true)";
	@SerializedName("armor_scales_with_difficulty")
	public boolean armorScalesWithDifficulty = true;

	@SerializedName("// Minimum world age in days before drowned can spawn with tridents (0 = no restriction)")
	public final String _comment_tmd = "(default: 10)";
	@SerializedName("trident_drowned_min_days")
	public int tridentDrownedMinDays = 10;

	public static WaterworldConfig loadConfigFile(File file) {
		WaterworldConfig config = null;
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				config = GSON.fromJson(reader, WaterworldConfig.class);
			} catch (IOException e) {
				throw new RuntimeException("[project-waterworld] Failed to load config: ", e);
			}
		}
		if (config == null) {
			config = new WaterworldConfig();
		}
		config.saveConfigFile(file);
		return config;
	}

	public void saveConfigFile(File file) {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			GSON.toJson(this, writer);
		} catch (IOException e) {
			ProjectWaterworld.LOGGER.error("Failed to save config", e);
		}
	}
}
