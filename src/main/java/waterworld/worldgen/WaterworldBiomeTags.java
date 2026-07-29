package waterworld.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import waterworld.WaterworldMod;

/**
 * Datapack biome tags used by Waterworld edge features (spawns, islands).
 * Add modded ocean biomes to these tags to opt into the matching behavior.
 */
public final class WaterworldBiomeTags {
	public static final TagKey<Biome> TURTLE_SPAWNS = tag("turtle_spawns");
	public static final TagKey<Biome> SPAWN_ISLAND_COLD = tag("spawn_island_cold");
	public static final TagKey<Biome> SPAWN_ISLAND_WARM = tag("spawn_island_warm");

	private WaterworldBiomeTags() {
	}

	private static TagKey<Biome> tag(String path) {
		return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, path));
	}
}
