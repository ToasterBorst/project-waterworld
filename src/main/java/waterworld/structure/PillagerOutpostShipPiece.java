package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * Pillager outpost ship piece. Pillagers are not placed at generation — the
 * garrison refills from datapack {@code spawn_overrides} via vanilla
 * {@code NaturalSpawner} (same as land outposts).
 */
public class PillagerOutpostShipPiece extends TemplateStructurePiece {

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.withDefaultNamespace("chests/pillager_outpost"));

	public PillagerOutpostShipPiece(StructureTemplateManager manager, Identifier template,
			BlockPos pos, RandomSource random) {
		super(WaterworldStructures.PILLAGER_OUTPOST_SHIP_PIECE, 0, manager, template,
				template.toString(), makeSettings(Rotation.getRandom(random)), pos);
	}

	public PillagerOutpostShipPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(WaterworldStructures.PILLAGER_OUTPOST_SHIP_PIECE, tag, context.structureTemplateManager(),
				id -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElse(Rotation.NONE)));
		// Legacy saves may have re-saved Rot=NONE while the placed blocks are rotated
		// (pre-Rot chunks loaded once by a Rot-saving build). The saved rotation can't
		// be trusted for coverage, so always widen to the union of all rotations —
		// water columns cost nothing, missing the real deck costs everything.
		this.boundingBox = boundingBoxForAnyRotation(this.template, this.templatePosition);
	}

	private static StructurePlaceSettings makeSettings(Rotation rotation) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setKnownShape(true)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	private static BoundingBox boundingBoxForAnyRotation(StructureTemplate template, BlockPos pos) {
		BoundingBox box = null;
		for (Rotation rotation : Rotation.values()) {
			BoundingBox next = template.getBoundingBox(makeSettings(rotation), pos);
			box = box == null ? next : BoundingBox.encapsulating(box, next);
		}
		return box;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
			RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		super.postProcess(level, structureManager, generator, random, chunkBox, chunkPos, pivot);
		clearFloodedAirBlocks(level, chunkBox);
		setChestLoot(level, chunkBox, random);
	}

	/**
	 * Restores template air that ocean water overwrote. With IGNORE_WATERLOGGING,
	 * exterior hull blocks stay waterlogged and interior dry blocks stay dry.
	 */
	private void clearFloodedAirBlocks(WorldGenLevel level, BoundingBox chunkBox) {
		clearFlooded(level, chunkBox, Blocks.AIR);
		clearFlooded(level, chunkBox, Blocks.CAVE_AIR);
	}

	private void clearFlooded(WorldGenLevel level, BoundingBox chunkBox,
			net.minecraft.world.level.block.Block airBlock) {
		List<StructureTemplate.StructureBlockInfo> airBlocks =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, airBlock);

		for (StructureTemplate.StructureBlockInfo info : airBlocks) {
			BlockPos worldPos = info.pos();
			if (!chunkBox.isInside(worldPos)) continue;

			BlockState existing = level.getBlockState(worldPos);
			if (existing.is(Blocks.WATER)) {
				level.setBlock(worldPos, airBlock.defaultBlockState(), 2);
			}
		}
	}

	private void setChestLoot(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		assignLoot(level, chunkBox, random, Blocks.CHEST);
		assignLoot(level, chunkBox, random, Blocks.TRAPPED_CHEST);
	}

	private void assignLoot(WorldGenLevel level, BoundingBox chunkBox, RandomSource random,
			net.minecraft.world.level.block.Block chestBlock) {
		List<StructureTemplate.StructureBlockInfo> chests =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, chestBlock);

		for (StructureTemplate.StructureBlockInfo info : chests) {
			BlockPos pos = info.pos();
			if (!chunkBox.isInside(pos)) continue;
			if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
				container.setLootTable(LOOT_TABLE, random.nextLong());
			}
		}
	}
}
