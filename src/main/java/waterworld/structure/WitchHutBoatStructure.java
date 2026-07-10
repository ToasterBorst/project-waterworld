package waterworld.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import waterworld.ProjectWaterworld;

import java.util.Map;
import java.util.Optional;

/**
 * Ocean witch hut structure — places our custom NBT boat at the water surface.
 * Bottom 4 blocks are submerged; interior air pockets and non-waterlogged blocks
 * are enforced in postProcess via explicit clearing.
 */
public class WitchHutBoatStructure extends Structure {

	public static final MapCodec<WitchHutBoatStructure> CODEC = simpleCodec(WitchHutBoatStructure::new);
	static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath(
			ProjectWaterworld.MOD_ID, "witch_hut_boat");
	private static final int SUBMERGED_BLOCKS = 4;

	public WitchHutBoatStructure(StructureSettings settings) {
		super(settings);
		Map<MobCategory, StructureSpawnOverride> overrides = settings.spawnOverrides();
		ProjectWaterworld.LOGGER.info("WitchHutBoat spawn_overrides: {}", overrides.isEmpty() ? "EMPTY" : overrides.keySet());
		for (var entry : overrides.entrySet()) {
			ProjectWaterworld.LOGGER.info("  {} -> {} entries", entry.getKey(),
					entry.getValue().spawns().unwrap().size());
		}
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

		int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
				x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

		int startY = surfaceY - SUBMERGED_BLOCKS + 1;

		BlockPos pos = new BlockPos(x, startY, z);

		WitchHutBoatPiece piece = new WitchHutBoatPiece(
				context.structureTemplateManager(), TEMPLATE, pos, context.random());
		builder.addPiece(piece);

		ProjectWaterworld.LOGGER.debug("WitchHutBoat piece at {} boundingBox={}", pos, piece.getBoundingBox());
	}

	@Override
	public StructureType<?> type() {
		return WaterworldStructures.WITCH_HUT_BOAT_TYPE;
	}
}
