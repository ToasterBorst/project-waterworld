package waterworld.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import waterworld.WaterworldConstants;

/**
 * Two-layer biome source that follows the vanilla seed as closely as possible.
 *
 * <p>The vanilla overworld biome is sampled first at every position; it is
 * kept verbatim whenever it is valid for its layer, and substituted with a
 * climate-matched alternative (same temperature/humidity/erosion/weirdness,
 * clamped continentalness) only when the layer demands it:
 *
 * <ul>
 *   <li><b>Surface layer</b> (block Y &gt;= sea level - fuzz): land, coast,
 *       river, and mushroom-field biomes are kept exactly where the vanilla
 *       seed places them; only ocean-tagged biomes are replaced with a
 *       climate-matched inland biome.</li>
 *   <li><b>Underwater layer</b>: ocean, cave, and underground biomes are kept;
 *       everything else (vanilla land) is replaced with a climate-matched
 *       ocean biome from the non-deep ocean band.</li>
 * </ul>
 *
 * <p>Classification is tag-driven (vanilla {@code minecraft:is_ocean} plus the
 * conventional {@code c:is_ocean}/{@code c:is_deep_ocean}/{@code c:is_cave}/
 * {@code c:is_underground} tags), so future vanilla and modded biomes that
 * carry correct tags are handled without code changes.
 */
public class WaterworldBiomeSource extends BiomeSource {
	public static final MapCodec<WaterworldBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		MultiNoiseBiomeSource.CODEC.fieldOf("overworld").forGetter(source -> source.overworld)
	).apply(instance, WaterworldBiomeSource::new));

	/**
	 * Continentalness floor for surface substitutions. Slightly inside the
	 * near-inland band (-0.11..0.03) rather than exactly on the coast boundary
	 * so nearest-match lookups don't tie between coast and inland entries.
	 */
	private static final long MIN_INLAND_CONTINENTALNESS = Climate.quantizeCoord(-0.10f);

	/**
	 * Underwater substitutions are clamped into the non-deep ocean band
	 * (-0.455..-0.19). Staying out of the deep-ocean band keeps ocean
	 * monuments and deep-ocean spawning away from columns that are land in
	 * the vanilla seed; genuine vanilla oceans keep their deep variants
	 * because ocean-tagged biomes are never substituted.
	 */
	private static final long OCEAN_BAND_MIN_CONTINENTALNESS = Climate.quantizeCoord(-0.45f);
	private static final long OCEAN_BAND_MAX_CONTINENTALNESS = Climate.quantizeCoord(-0.2f);

	/**
	 * Biome noise is fuzzed by up to a quart; treating the top water quart as
	 * part of the surface layer guarantees the water at and just below sea
	 * level renders with the surface biome's water color, fog, and sky.
	 */
	private static final int BIOME_FUZZ_BUFFER = 4;

	private final MultiNoiseBiomeSource overworld;

	public WaterworldBiomeSource(MultiNoiseBiomeSource overworld) {
		this.overworld = overworld;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return this.overworld.possibleBiomes().stream();
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		Holder<Biome> biome = this.overworld.getNoiseBiome(quartX, quartY, quartZ, sampler);
		int blockY = quartToBlockY(quartY);

		if (blockY >= WaterworldConstants.SEA_LEVEL - BIOME_FUZZ_BUFFER) {
			if (!isOcean(biome)) {
				return biome;
			}
			return this.overworld.getNoiseBiome(clampToInland(sampler.sample(quartX, quartY, quartZ)));
		}

		if (isOcean(biome) || isCaveOrUnderground(biome)) {
			return biome;
		}
		return this.overworld.getNoiseBiome(clampToOcean(sampler.sample(quartX, quartY, quartZ)));
	}

	@Override
	public Set<Holder<Biome>> getBiomesWithin(int blockX, int blockY, int blockZ, int blockRadius, Climate.Sampler sampler) {
		if (blockY > WaterworldConstants.SEA_LEVEL) {
			return super.getBiomesWithin(blockX, blockY, blockZ, blockRadius, sampler);
		}
		int minQX = QuartPos.fromBlock(blockX - blockRadius);
		int maxQX = QuartPos.fromBlock(blockX + blockRadius);
		int minQZ = QuartPos.fromBlock(blockZ - blockRadius);
		int maxQZ = QuartPos.fromBlock(blockZ + blockRadius);
		int quartY = QuartPos.fromBlock(blockY);

		Set<Holder<Biome>> biomes = new HashSet<>();
		for (int qx = minQX; qx <= maxQX; qx++) {
			for (int qz = minQZ; qz <= maxQZ; qz++) {
				biomes.add(this.getNoiseBiome(qx, quartY, qz, sampler));
			}
		}
		return biomes;
	}

	private static boolean isOcean(Holder<Biome> biome) {
		return biome.is(BiomeTags.IS_OCEAN)
			|| biome.is(ConventionalBiomeTags.IS_OCEAN)
			|| biome.is(ConventionalBiomeTags.IS_DEEP_OCEAN);
	}

	private static boolean isCaveOrUnderground(Holder<Biome> biome) {
		return biome.is(ConventionalBiomeTags.IS_CAVE)
			|| biome.is(ConventionalBiomeTags.IS_UNDERGROUND);
	}

	/**
	 * Raises continentalness to at least the near-inland threshold, so the
	 * overworld preset picks a climate-matched land biome for the air column
	 * above vanilla oceans.
	 */
	private static Climate.TargetPoint clampToInland(Climate.TargetPoint point) {
		long c = point.continentalness();
		if (c >= MIN_INLAND_CONTINENTALNESS) {
			return point;
		}
		return new Climate.TargetPoint(point.temperature(), point.humidity(),
			MIN_INLAND_CONTINENTALNESS, point.erosion(), point.depth(), point.weirdness());
	}

	/**
	 * Clamps continentalness into the non-deep ocean band, so the overworld
	 * preset picks a climate-matched ocean biome underwater where the vanilla
	 * seed has land (including the mushroom-field band below -1.05).
	 */
	private static Climate.TargetPoint clampToOcean(Climate.TargetPoint point) {
		long c = point.continentalness();
		long clamped = Mth.clamp(c, OCEAN_BAND_MIN_CONTINENTALNESS, OCEAN_BAND_MAX_CONTINENTALNESS);
		if (clamped == c) {
			return point;
		}
		return new Climate.TargetPoint(point.temperature(), point.humidity(),
			clamped, point.erosion(), point.depth(), point.weirdness());
	}

	private static int quartToBlockY(int quartY) {
		return quartY << 2;
	}
}
