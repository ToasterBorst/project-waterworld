package waterworld.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Shared ocean monument presence checks for wild guardian / rider spawn rules.
 */
public final class OceanMonumentChecks {
	private OceanMonumentChecks() {
	}

	public static boolean isInOceanMonument(ServerLevel level, BlockPos pos) {
		Structure monument = level.registryAccess()
				.lookupOrThrow(Registries.STRUCTURE)
				.getValue(BuiltinStructures.OCEAN_MONUMENT);
		if (monument == null) return false;

		StructureStart start = level.structureManager().getStructureWithPieceAt(pos, monument);
		return start.isValid();
	}
}
