package waterworld.mixin;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldDetection;

/**
 * Prevents the ocean monument's terrain-clearing generateWaterBox calls from
 * carving rectangular holes in the seabed.
 *
 * Two categories of destructive calls are blocked:
 *
 * 1) The initial full-volume clear: generateWaterBox(0,0,0, 58,y2,58)
 *    which clears the entire 58x58 bounding box up to sea level.
 *    Detected by: both X and Z spans >= 50.
 *
 * 2) The stepped border clears that form a pyramid around the perimeter:
 *    A loop creates 1-block-thick vertical planes extending 5 blocks outward
 *    in each direction (e.g. generateWaterBox(-1-i, i*2, -1-i, -1-i, 23, 58+i)).
 *    These have one dimension span of 0 and the other >= 57.
 *    Detected by: one span == 0 and the other >= 50.
 *
 * Interior room/wing clears (needed for functional structure) have both
 * dimensions well under 50 and proceed normally.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentPiece")
public abstract class OceanMonumentTerrainMixin {

	@Inject(method = "generateWaterBox", at = @At("HEAD"), cancellable = true)
	private void waterworld$skipDestructiveClears(WorldGenLevel level, BoundingBox chunkBB,
			int x1, int y1, int z1, int x2, int y2, int z2, CallbackInfo ci) {
		if (!WaterworldDetection.isActive()) return;

		int xSpan = x2 - x1;
		int zSpan = z2 - z1;

		if (xSpan >= 50 && zSpan >= 50) {
			ci.cancel();
			return;
		}

		if ((xSpan == 0 && zSpan >= 50) || (zSpan == 0 && xSpan >= 50)) {
			ci.cancel();
		}
	}
}
