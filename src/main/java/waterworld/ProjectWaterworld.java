package waterworld;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import waterworld.spawn.WaterworldSpawns;
import waterworld.worldgen.WaterworldBiomeSources;

import java.io.File;

public class ProjectWaterworld implements ModInitializer {
	public static final String MOD_ID = "project-waterworld";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Project Waterworld");

		File configFile = FabricLoader.getInstance().getConfigDir().resolve("project-waterworld.json").toFile();
		WaterworldConfig.INSTANCE = WaterworldConfig.loadConfigFile(configFile);
		LOGGER.info("Loaded Waterworld config");

		WaterworldBiomeSources.register();
		WaterworldSpawns.register();

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
