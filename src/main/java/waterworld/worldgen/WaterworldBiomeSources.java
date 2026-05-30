package waterworld.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import waterworld.ProjectWaterworld;

public final class WaterworldBiomeSources {
	private WaterworldBiomeSources() {
	}

	public static void register() {
		Registry.register(
			BuiltInRegistries.BIOME_SOURCE,
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "waterworld"),
			WaterworldBiomeSource.CODEC
		);
		ProjectWaterworld.LOGGER.info("Registered Waterworld biome source");
	}
}
