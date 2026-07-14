package waterworld.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConstants;

import java.util.Optional;

/**
 * Pillager outpost ship — places one of three NBT variants at fixed sea level.
 * Horizontal position follows the structure set / world seed; Y is always
 * {@link WaterworldConstants#SEA_LEVEL}. Bottom 8 blocks of the template are
 * submerged so the waterline in the NBT aligns with the ocean surface.
 */
public class PillagerOutpostShipStructure extends Structure {

	public static final MapCodec<PillagerOutpostShipStructure> CODEC =
			simpleCodec(PillagerOutpostShipStructure::new);

	static final Identifier[] TEMPLATES = {
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "pillager_outpost_ship_basic"),
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "pillager_outpost_ship_allay"),
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "pillager_outpost_ship_golem")
	};

	/** Template local Y 0–7 are ocean water; deck begins at local Y 8. */
	private static final int SUBMERGED_BLOCKS = 8;

	public PillagerOutpostShipStructure(StructureSettings settings) {
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

		// Align NBT waterline (local Y 0–7) with sea level; no +1 — ships sat 1 block high.
		int startY = WaterworldConstants.SEA_LEVEL - SUBMERGED_BLOCKS;
		BlockPos pos = new BlockPos(x, startY, z);

		RandomSource random = context.random();
		Identifier template = TEMPLATES[random.nextInt(TEMPLATES.length)];

		PillagerOutpostShipPiece piece = new PillagerOutpostShipPiece(
				context.structureTemplateManager(), template, pos, random);
		builder.addPiece(piece);

		ProjectWaterworld.LOGGER.debug("PillagerOutpostShip {} at {} boundingBox={}",
				template, pos, piece.getBoundingBox());
	}

	@Override
	public StructureType<?> type() {
		return WaterworldStructures.PILLAGER_OUTPOST_SHIP_TYPE;
	}
}
