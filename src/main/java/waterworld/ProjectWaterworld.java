package waterworld;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import waterworld.spawn.WaterworldSpawns;
import waterworld.structure.WaterworldStructures;
import waterworld.worldgen.WaterworldBiomeSources;

import java.nio.file.Path;

public class ProjectWaterworld implements ModInitializer {
	public static final String MOD_ID = "project-waterworld";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Waterworld");

		Path configDir = FabricLoader.getInstance().getConfigDir();
		WaterworldConfig.INSTANCE = WaterworldConfig.load(configDir);
		// Persist new keys (sea_level, drowned charge) and migrated defaults.
		WaterworldConfig.INSTANCE.save(configDir);
		LOGGER.info("Loaded Waterworld config (sea_level={}, drownedCharge={}/{})",
				WaterworldConfig.INSTANCE.seaLevel,
				WaterworldConfig.INSTANCE.drownedSpawnCharge,
				WaterworldConfig.INSTANCE.drownedSpawnEnergyBudget);

		WaterworldBiomeSources.register();
		WaterworldStructures.register();
		WaterworldSpawns.register();

		ServerLifecycleEvents.SERVER_STARTED.register(WaterworldDetection::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(WaterworldDetection::onServerStopped);
	}
}
