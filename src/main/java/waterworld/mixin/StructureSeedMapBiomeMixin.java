package waterworld.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import waterworld.worldgen.WaterworldBiomeSource;

/**
 * Structure placement biome gates must follow the vanilla seed map.
 *
 * <p>Waterworld remaps biomes by Y (surface inland / underwater ocean fill).
 * Structure stubs often sample at ocean-floor or water-surface Y, which would
 * accept remapped oceans under former land (inflating wrecks/ruins/portals)
 * or reject land tags under the sea (deflating trail ruins). Redirect the
 * check to the wrapped vanilla overworld climate at seed-map depth instead.
 */
@Mixin(Structure.class)
public class StructureSeedMapBiomeMixin {

	@Redirect(
			method = "isValidBiome",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/biome/BiomeSource;getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"
			)
	)
	private static Holder<Biome> waterworld$seedMapBiome(
			BiomeSource source, int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		if (source instanceof WaterworldBiomeSource waterworld) {
			return waterworld.sampleVanillaOverworldAtSeedMapDepth(quartX, quartZ, sampler);
		}
		return source.getNoiseBiome(quartX, quartY, quartZ, sampler);
	}
}
