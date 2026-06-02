package waterworld.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import waterworld.WaterworldConstants;

public class WaterworldBiomeSource extends BiomeSource {
	public static final MapCodec<WaterworldBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		MultiNoiseBiomeSource.CODEC.fieldOf("underwater").forGetter(source -> source.underwater),
		MultiNoiseBiomeSource.CODEC.fieldOf("land").forGetter(source -> source.land)
	).apply(instance, WaterworldBiomeSource::new));

	private static final long MIN_INLAND_CONTINENTALNESS = Climate.quantizeCoord(-0.11f);
	private static final int BIOME_FUZZ_BUFFER = 4;

	private final MultiNoiseBiomeSource underwater;
	private final MultiNoiseBiomeSource land;

	public WaterworldBiomeSource(MultiNoiseBiomeSource underwater, MultiNoiseBiomeSource land) {
		this.underwater = underwater;
		this.land = land;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		Set<Holder<Biome>> biomes = new HashSet<>();
		biomes.addAll(this.underwater.possibleBiomes());
		biomes.addAll(this.land.possibleBiomes());
		return biomes.stream();
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		int blockY = quartToBlockY(quartY);
		if (blockY >= WaterworldConstants.SEA_LEVEL - BIOME_FUZZ_BUFFER) {
			Climate.TargetPoint point = sampler.sample(quartX, quartY, quartZ);
			return this.land.getNoiseBiome(clampToInland(point));
		}
		return this.underwater.getNoiseBiome(quartX, quartY, quartZ, sampler);
	}

	/**
	 * Overrides the parent's getBiomesWithin which takes BLOCK coordinates.
	 * For queries at or below sea level, we only sample the underwater source
	 * at the given Y (preventing the vertical radius from bleeding into the
	 * land layer above sea level and contaminating structure biome checks).
	 */
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
				biomes.add(this.underwater.getNoiseBiome(qx, quartY, qz, sampler));
			}
		}
		return biomes;
	}

	/**
	 * Clamps the continentalness of a sampled target point to at least the
	 * near-inland threshold (-0.11), preventing the vanilla overworld preset
	 * from selecting ocean/coast/beach biomes in the air column above sea level.
	 */
	private static Climate.TargetPoint clampToInland(Climate.TargetPoint point) {
		long c = point.continentalness();
		if (c >= MIN_INLAND_CONTINENTALNESS) {
			return point;
		}
		return new Climate.TargetPoint(point.temperature(), point.humidity(),
			MIN_INLAND_CONTINENTALNESS, point.erosion(), point.depth(), point.weirdness());
	}

	private static int quartToBlockY(int quartY) {
		return quartY << 2;
	}
}
