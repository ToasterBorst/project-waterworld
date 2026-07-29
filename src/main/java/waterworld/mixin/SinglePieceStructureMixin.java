package waterworld.mixin;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import waterworld.WaterworldDetection;

/**
 * Desert pyramids / jungle temples reject any candidate where corner
 * {@code WORLD_SURFACE_WG} height is below sea level. In Waterworld that
 * height is always the water top ({@code seaLevel - 1}), so every candidate
 * returns empty and {@code /locate} never finishes.
 *
 * <p>Bypass the dry-land gate in Waterworld. Piece settle to the solid ocean
 * floor and interior flooding remain handled by
 * {@link ScatteredFeatureSeabedMixin} and {@link FloodedScatteredStructureMixin}.
 */
@Mixin(SinglePieceStructure.class)
public abstract class SinglePieceStructureMixin {

	@Redirect(
			method = "findGenerationPoint",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getSeaLevel()I"
			)
	)
	private int waterworld$bypassDryLandSeaGate(ChunkGenerator generator) {
		if (WaterworldDetection.usesWaterworldBiomeSource(generator)) {
			// getLowestY < seaLevel must never reject underwater candidates.
			return Integer.MIN_VALUE;
		}
		return generator.getSeaLevel();
	}
}
