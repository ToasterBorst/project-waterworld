package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldDetection;
import waterworld.structure.SpawnZonePiece;

/**
 * For {@link SpawnZonePiece}s, {@code spawn_overrides} with {@code bounding_box: piece}
 * must use the vanilla-sized spawn zone — not the full template placement BB.
 */
@Mixin(StructureManager.class)
public class StructureManagerMixin {

	@Inject(method = "structureHasPieceAt", at = @At("HEAD"), cancellable = true)
	private void waterworld$spawnZonePieceCheck(
			BlockPos blockPos, StructureStart structureStart, CallbackInfoReturnable<Boolean> cir) {
		if (!WaterworldDetection.isActive()) return;

		boolean sawSpawnZonePiece = false;
		for (StructurePiece piece : structureStart.getPieces()) {
			if (piece instanceof SpawnZonePiece spawnZonePiece) {
				sawSpawnZonePiece = true;
				if (spawnZonePiece.getSpawnZone().isInside(blockPos)) {
					cir.setReturnValue(true);
					return;
				}
			} else if (piece.getBoundingBox().isInside(blockPos)) {
				cir.setReturnValue(true);
				return;
			}
		}

		if (sawSpawnZonePiece) {
			cir.setReturnValue(false);
		}
	}
}
