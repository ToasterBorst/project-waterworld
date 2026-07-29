package waterworld.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import waterworld.WaterworldMod;

public final class WaterworldBiomeSources {
	private WaterworldBiomeSources() {
	}

	public static void register() {
		Registry.register(
			BuiltInRegistries.BIOME_SOURCE,
			Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "waterworld"),
			WaterworldBiomeSource.CODEC
		);
		WaterworldMod.LOGGER.info("Registered Waterworld biome source");
	}
}
