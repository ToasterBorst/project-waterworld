package waterworld.spawn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.worldgen.WaterworldBiomeTags;

public final class WaterworldSpawns {
	private WaterworldSpawns() {
	}

	public static void register() {
		registerGuardianSpawns();
		registerTurtleSpawns();
		registerEntityLoadEvents();
		SpawnGearHandler.register();
		ProjectWaterworld.LOGGER.info("Registered Waterworld spawn modifications");
	}

	private static void registerGuardianSpawns() {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.wildGuardianSpawns) return;

		BiomeModifications.addSpawn(
			BiomeSelectors.tag(BiomeTags.IS_OCEAN),
			MobCategory.MONSTER,
			EntityTypes.GUARDIAN,
			config.guardianSpawnWeight, 1, 1
		);
	}

	private static void registerTurtleSpawns() {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.turtleOceanSpawns) return;

		BiomeModifications.addSpawn(
			BiomeSelectors.tag(WaterworldBiomeTags.TURTLE_SPAWNS),
			MobCategory.CREATURE,
			EntityTypes.TURTLE,
			config.turtleSpawnWeight, 2, 5
		);
	}

	private static void registerEntityLoadEvents() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(level instanceof ServerLevel)) return;
			if (!WaterworldDetection.isActive()) return;
			if (!(entity instanceof Mob mob)) return;

			injectBoatGoals(mob);
			injectDoorGoals(mob);
		});
	}

	private static void injectBoatGoals(Mob mob) {
		boolean canDismount = WaterworldMobTypes.canDismountBoats(mob);
		boolean canPilot = WaterworldMobTypes.canPilotBoats(mob);
		if (!canDismount && !canPilot) return;
		BoatSpawnHelper.addBoatAI(mob, canPilot);
	}

	private static void injectDoorGoals(Mob mob) {
		if (!WaterworldMobTypes.shouldOpenDoors(mob)) return;
		if (hasDoorGoal(mob)) return;

		mob.getNavigation().setCanOpenDoors(true);
		mob.goalSelector.addGoal(3, new OpenDoorGoal(mob, true));
	}

	private static boolean hasDoorGoal(Mob mob) {
		return mob.goalSelector.getAvailableGoals().stream()
				.anyMatch(wg -> wg.getGoal() instanceof OpenDoorGoal);
	}
}
