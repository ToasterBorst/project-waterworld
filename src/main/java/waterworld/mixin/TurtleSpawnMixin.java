package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.worldgen.WaterworldBiomeTags;

/**
 * Allows turtles to spawn on underwater sand in biomes tagged #project-waterworld:turtle_spawns.
 */
@Mixin(Turtle.class)
public class TurtleSpawnMixin {

	@Inject(method = "checkTurtleSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void waterworld$allowOceanTurtleSpawns(
			EntityType<? extends Turtle> type, LevelAccessor level,
			EntitySpawnReason spawnReason, BlockPos pos, RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		if (!WaterworldDetection.isActive()) return;
		if (!WaterworldConfig.INSTANCE.turtleOceanSpawns) return;
		if (spawnReason != EntitySpawnReason.NATURAL) return;
		if (pos.getY() >= level.getSeaLevel() + 4) return;
		if (!TurtleEggBlock.onSand(level, pos)) return;

		Holder<Biome> biome = level.getBiome(pos);
		if (biome.is(WaterworldBiomeTags.TURTLE_SPAWNS)) {
			cir.setReturnValue(true);
		}
	}
}
