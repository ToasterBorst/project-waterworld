package waterworld.structure;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import waterworld.ProjectWaterworld;

public final class WaterworldStructures {

	public static final ResourceKey<Structure> WITCH_HUT_BOAT_KEY = ResourceKey.create(
			Registries.STRUCTURE,
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "witch_hut_boat"));

	public static final StructurePieceType WITCH_HUT_BOAT_PIECE = WitchHutBoatPiece::new;

	public static final StructureType<WitchHutBoatStructure> WITCH_HUT_BOAT_TYPE =
			() -> WitchHutBoatStructure.CODEC;

	private WaterworldStructures() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
				Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "witch_hut_boat"),
				WITCH_HUT_BOAT_TYPE);

		Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
				Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "witch_hut_boat"),
				WITCH_HUT_BOAT_PIECE);

		ProjectWaterworld.LOGGER.info("Registered Waterworld structures");
	}
}
