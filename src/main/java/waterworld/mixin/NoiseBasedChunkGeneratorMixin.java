package waterworld.mixin;

import com.google.common.base.Suppliers;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.worldgen.WaterworldBiomeSource;

import java.util.function.Supplier;

/**
 * Makes the {@code sea_level} config the source of truth for Waterworld worlds:
 * both {@link NoiseBasedChunkGenerator#getSeaLevel()} queries (drowned depth,
 * spawn helpers) and new-chunk fluid placement follow the config instead of the
 * JSON baked into the jar. Gated on {@link WaterworldBiomeSource} so other
 * dimensions and world types are untouched.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

	@Shadow
	@Final
	private Holder<NoiseGeneratorSettings> settings;

	@Mutable
	@Shadow
	@Final
	private Supplier<Aquifer.FluidPicker> globalFluidPicker;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void waterworld$configSeaLevelFluids(
			BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, CallbackInfo ci) {
		if (!(biomeSource instanceof WaterworldBiomeSource)) return;

		// The constructor runs during registry decoding, when the settings holder is
		// still unbound — settings.value() would throw. Defer like vanilla does and
		// only resolve inside the (memoized) supplier, which runs during worldgen.
		this.globalFluidPicker = Suppliers.memoize(() -> {
			int configSea = WaterworldConfig.INSTANCE.seaLevel;
			Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
			Aquifer.FluidStatus water =
					new Aquifer.FluidStatus(configSea, settings.value().defaultFluid());
			int lavaCeiling = Math.min(-54, configSea);
			return (x, y, z) -> y < lavaCeiling ? lava : water;
		});
	}

	@Inject(method = "getSeaLevel", at = @At("HEAD"), cancellable = true)
	private void waterworld$configSeaLevel(CallbackInfoReturnable<Integer> cir) {
		NoiseBasedChunkGenerator self = (NoiseBasedChunkGenerator) (Object) this;
		if (!(self.getBiomeSource() instanceof WaterworldBiomeSource)) return;
		cir.setReturnValue(WaterworldConfig.INSTANCE.seaLevel);
	}
}
