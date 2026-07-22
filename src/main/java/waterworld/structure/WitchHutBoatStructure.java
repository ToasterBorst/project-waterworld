package waterworld.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConstants;

import java.util.Optional;

/**
 * Ocean witch hut structure — places our custom NBT boat at configured sea level.
 * Bottom 4 blocks are submerged; interior air is enforced in postProcess.
 */
public class WitchHutBoatStructure extends Structure {

	public static final MapCodec<WitchHutBoatStructure> CODEC = simpleCodec(WitchHutBoatStructure::new);
	static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath(
			ProjectWaterworld.MOD_ID, "witch_hut_boat");
	/** Template local Y 0–3 are ocean water; deck begins at local Y 4. */
	private static final int SUBMERGED_BLOCKS = 4;

	public WitchHutBoatStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
			generatePieces(builder, context);
		});
	}

	private void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
		ChunkPos chunkPos = context.chunkPos();
		int x = chunkPos.getMiddleBlockX();
		int z = chunkPos.getMiddleBlockZ();

		// Match outpost ships: sea-level anchor, no heightmap +1 drift.
		int startY = WaterworldConstants.seaLevel() - SUBMERGED_BLOCKS;
		BlockPos pos = new BlockPos(x, startY, z);

		WitchHutBoatPiece piece = new WitchHutBoatPiece(
				context.structureTemplateManager(), TEMPLATE, pos, context.random());
		builder.addPiece(piece);

		ProjectWaterworld.LOGGER.debug("WitchHutBoat piece at {} boundingBox={} spawnZone={}",
				pos, piece.getBoundingBox(), piece.getSpawnZone());
	}

	@Override
	public StructureType<?> type() {
		return WaterworldStructures.WITCH_HUT_BOAT_TYPE;
	}
}
