package waterworld.spawn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.level.biome.Biomes;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

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
			BiomeSelectors.includeByKey(Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN),
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

			WaterworldConfig config = WaterworldConfig.INSTANCE;
			injectBoatGoals(mob, config);
		});
	}

	private static void injectBoatGoals(Mob mob, WaterworldConfig config) {
		boolean canDismount = mob instanceof Pillager
				|| mob instanceof Vindicator
				|| mob instanceof Evoker
				|| mob instanceof Witch
				|| mob instanceof Ravager
				|| mob instanceof WanderingTrader
				|| mob instanceof TraderLlama;

		boolean canPilot = mob instanceof Pillager
				|| mob instanceof Vindicator
				|| mob instanceof Evoker
				|| mob instanceof Witch
				|| mob instanceof WanderingTrader;

		if (!canDismount && !canPilot) return;

		BoatSpawnHelper.addBoatAI(mob, canPilot);
	}
}
