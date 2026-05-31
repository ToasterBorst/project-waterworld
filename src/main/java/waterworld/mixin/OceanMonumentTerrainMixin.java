package waterworld.mixin;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the ocean monument's initial full-volume water clear from
 * carving a massive rectangular hole in the seabed.
 *
 * MonumentBuilding.postProcess() first calls generateWaterBox(0,0,0, 58,y2,58)
 * which clears the ENTIRE 58x58 bounding box up to sea level. In vanilla this
 * is harmless (already water), but in our waterworld it destroys terrain.
 *
 * We cancel any generateWaterBox call with a footprint >= 50x50 blocks.
 * The monument's wing and room pieces use much smaller clears for their
 * interiors, so those proceed normally and the structure remains functional.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentPiece")
public abstract class OceanMonumentTerrainMixin {

	@Inject(method = "generateWaterBox", at = @At("HEAD"), cancellable = true)
	private void waterworld$skipFullVolumeClear(WorldGenLevel level, BoundingBox chunkBB,
			int x1, int y1, int z1, int x2, int y2, int z2, CallbackInfo ci) {
		if ((x2 - x1) >= 50 && (z2 - z1) >= 50) {
			ci.cancel();
		}
	}
}
