package waterworld.spawn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.PathType;
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
		registerDrownedSpawnCost();
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

	/**
	 * Vanilla spawn_costs for ocean drowned — limits local packing via
	 * {@code SpawnState.canSpawn} without a custom tick loop.
	 */
	private static void registerDrownedSpawnCost() {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (config.drownedSpawnCharge <= 0.0 || config.drownedSpawnEnergyBudget <= 0.0) {
			return;
		}

		double charge = config.drownedSpawnCharge;
		double budget = config.drownedSpawnEnergyBudget;

		BiomeModifications.create(Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "drowned_spawn_cost"))
				.add(ModificationPhase.ADDITIONS, BiomeSelectors.tag(BiomeTags.IS_OCEAN), context ->
						context.getMobSpawnSettings().addMobCharge(EntityTypes.DROWNED, charge, budget));

		ProjectWaterworld.LOGGER.info(
				"Registered ocean drowned MobSpawnCost charge={} energyBudget={}", charge, budget);
	}

	private static void registerEntityLoadEvents() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(level instanceof ServerLevel serverLevel)) return;
			if (!WaterworldDetection.isActive()) return;
			if (!(entity instanceof Mob mob)) return;

			if (!WaterworldMobTypes.canDismountBoats(mob)
					&& !WaterworldMobTypes.canPilotBoats(mob)
					&& !WaterworldMobTypes.shouldOpenDoors(mob)
					&& !WaterworldMobTypes.isHostileBoatPilot(mob)
					&& !MobEquipmentHelper.shouldEquipArmor(mob)) {
				return;
			}

			injectBoatGoals(mob);
			injectDoorGoals(mob);
			injectWaterAvoidance(mob);
			MobEquipmentHelper.tryEquipArmorOnLoad(mob, serverLevel);
		});
	}

	private static void injectBoatGoals(Mob mob) {
		if (!WaterworldMobTypes.canDismountBoats(mob) && !WaterworldMobTypes.canPilotBoats(mob)) {
			return;
		}
		// Base goals only; spawn sites add pilot goals with the correct role.
		BoatSpawnHelper.addBaseBoatGoals(mob);
	}

	private static void injectDoorGoals(Mob mob) {
		if (!WaterworldMobTypes.shouldOpenDoors(mob)) return;
		if (mob instanceof Villager) return;
		if (hasDoorGoal(mob)) return;

		mob.getNavigation().setCanOpenDoors(true);
		mob.goalSelector.addGoal(3, new OpenDoorGoal(mob, true));
	}

	/**
	 * Ground illagers path off ship decks into the ocean because water landings
	 * within fall distance are acceptable at the default WATER malus (8). Block
	 * water pathing like Bee/EnderMan do; SwimToLandGoal and MountNearbyBoatGoal
	 * temporarily lift the malus while the mob is actually swimming.
	 */
	private static void injectWaterAvoidance(Mob mob) {
		if (!WaterworldMobTypes.isHostileBoatPilot(mob)) return;

		mob.setPathfindingMalus(PathType.WATER, -1.0F);
		mob.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);

		// Vanilla pattern (Witch/Ravager/Zombie): idle strolls that avoid water.
		if (mob instanceof PathfinderMob pathfinderMob) {
			WrappedGoal plainStroll = null;
			for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
				if (wg.getGoal().getClass() == RandomStrollGoal.class) {
					plainStroll = wg;
					break;
				}
			}
			if (plainStroll != null) {
				mob.goalSelector.removeGoal(plainStroll.getGoal());
				mob.goalSelector.addGoal(plainStroll.getPriority(),
						new WaterAvoidingRandomStrollGoal(pathfinderMob, 0.6));
			}
		}
	}

	private static boolean hasDoorGoal(Mob mob) {
		for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
			if (wg.getGoal() instanceof OpenDoorGoal) {
				return true;
			}
		}
		return false;
	}
}
