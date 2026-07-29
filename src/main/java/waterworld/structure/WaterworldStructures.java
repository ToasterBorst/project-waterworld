package waterworld.structure;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import waterworld.WaterworldMod;

/**
 * Registers custom structure <em>types</em> and piece codecs. The datapack
 * structure definitions themselves override vanilla IDs ({@code minecraft:swamp_hut},
 * {@code minecraft:pillager_outpost}) so {@code /locate} and seed maps use
 * the familiar names while placing Waterworld NBT substitutes.
 */
public final class WaterworldStructures {

	public static final ResourceKey<Structure> SWAMP_HUT_KEY = ResourceKey.create(
			Registries.STRUCTURE,
			Identifier.withDefaultNamespace("swamp_hut"));

	public static final ResourceKey<Structure> PILLAGER_OUTPOST_KEY = ResourceKey.create(
			Registries.STRUCTURE,
			Identifier.withDefaultNamespace("pillager_outpost"));

	public static final StructurePieceType WITCH_HUT_BOAT_PIECE = WitchHutBoatPiece::new;

	public static final StructurePieceType PILLAGER_OUTPOST_SHIP_PIECE = PillagerOutpostShipPiece::new;

	public static final StructureType<WitchHutBoatStructure> WITCH_HUT_BOAT_TYPE =
			() -> WitchHutBoatStructure.CODEC;

	public static final StructureType<PillagerOutpostShipStructure> PILLAGER_OUTPOST_SHIP_TYPE =
			() -> PillagerOutpostShipStructure.CODEC;

	private WaterworldStructures() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
				Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "witch_hut_boat"),
				WITCH_HUT_BOAT_TYPE);

		Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
				Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "witch_hut_boat"),
				WITCH_HUT_BOAT_PIECE);

		Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
				Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "pillager_outpost_ship"),
				PILLAGER_OUTPOST_SHIP_TYPE);

		Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
				Identifier.fromNamespaceAndPath(WaterworldMod.MOD_ID, "pillager_outpost_ship"),
				PILLAGER_OUTPOST_SHIP_PIECE);

		WaterworldMod.LOGGER.info("Registered Waterworld structures");
	}
}
