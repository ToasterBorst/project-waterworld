package waterworld.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
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
		if (blockY > WaterworldConstants.SEA_LEVEL) {
			return this.land.getNoiseBiome(quartX, quartY, quartZ, sampler);
		}
		return this.underwater.getNoiseBiome(quartX, quartY, quartZ, sampler);
	}

	@Override
	public Set<Holder<Biome>> getBiomesWithin(int quartX, int quartY, int quartZ, int radius, Climate.Sampler sampler) {
		int blockY = quartToBlockY(quartY);
		if (blockY > WaterworldConstants.SEA_LEVEL) {
			return this.land.getBiomesWithin(quartX, quartY, quartZ, radius, sampler);
		}
		return this.underwater.getBiomesWithin(quartX, quartY, quartZ, radius, sampler);
	}

	private static int quartToBlockY(int quartY) {
		return quartY << 2;
	}
}
