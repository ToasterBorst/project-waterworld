package waterworld;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import waterworld.spawn.WaterworldSpawns;
import waterworld.structure.WaterworldStructures;
import waterworld.structure.WitchHutBoatSpawner;
import waterworld.worldgen.WaterworldBiomeSources;

import java.nio.file.Path;

public class ProjectWaterworld implements ModInitializer {
	public static final String MOD_ID = "project-waterworld";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final WitchHutBoatSpawner witchHutBoatSpawner = new WitchHutBoatSpawner();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Project Waterworld");

		Path configDir = FabricLoader.getInstance().getConfigDir();
		WaterworldConfig.INSTANCE = WaterworldConfig.load(configDir);
		LOGGER.info("Loaded Waterworld config");

		WaterworldBiomeSources.register();
		WaterworldStructures.register();
		WaterworldSpawns.register();

		ServerLifecycleEvents.SERVER_STARTED.register(WaterworldDetection::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(WaterworldDetection::onServerStopped);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var level : server.getAllLevels()) {
				witchHutBoatSpawner.tick(level);
			}
		});

		try {
			Identifier packId = Identifier.fromNamespaceAndPath(MOD_ID, "waterworld");

			ResourceManagerHelper.registerBuiltinResourcePack(
				packId,
				FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(),
				ResourcePackActivationType.DEFAULT_ENABLED
			);

			LOGGER.info("Registered Waterworld datapack");
		} catch (Exception e) {
			LOGGER.error("Failed to register datapack: {}", e.getMessage());
		}
	}
}
