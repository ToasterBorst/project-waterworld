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
 * Pillager outpost ship piece. Pillagers refill from datapack {@code spawn_overrides}.
 * Placement BB is the full rotated template (multi-chunk safe). Natural pillager
 * overrides use a separate vanilla-sized {@code 72×58×72} spawn zone centered on
 * the command-deck chest (wiki watchtower-top analogue).
 */
public class PillagerOutpostShipPiece extends TemplateStructurePiece implements SpawnZonePiece {

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.withDefaultNamespace("chests/pillager_outpost"));

	/** Highest chest — command-deck / watchtower-top analogue (local template coords). */
	private static final BlockPos COMMAND_DECK_ANCHOR_LOCAL = new BlockPos(7, 12, 56);

	/** Wiki Java outpost spawn box: 72×58×72, 30 down / 28 up from platform center. */
	private static final int SPAWN_SIZE_XZ = 72;
	private static final int SPAWN_BELOW = 30;
	private static final int SPAWN_ABOVE = 28;

	private BoundingBox spawnZone;

	public PillagerOutpostShipPiece(StructureTemplateManager manager, Identifier template,
			BlockPos pos, RandomSource random) {
		super(WaterworldStructures.PILLAGER_OUTPOST_SHIP_PIECE, 0, manager, template,
				template.toString(), makeSettings(Rotation.getRandom(random)), pos);
		this.spawnZone = computeSpawnZone();
	}

	public PillagerOutpostShipPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(WaterworldStructures.PILLAGER_OUTPOST_SHIP_PIECE, tag, context.structureTemplateManager(),
				id -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElse(Rotation.NONE)));
		this.spawnZone = readSpawnZone(tag).orElseGet(this::computeSpawnZone);
		// Keep placement BB as the real template footprint (multi-chunk intersects).
		this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
	}

	private static StructurePlaceSettings makeSettings(Rotation rotation) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setKnownShape(true)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	private BoundingBox computeSpawnZone() {
		BlockPos center = StructureSpawnZones.localToWorld(
				this.templatePosition, this.placeSettings.getRotation(), COMMAND_DECK_ANCHOR_LOCAL);
		return StructureSpawnZones.boxCentered(
				center, SPAWN_SIZE_XZ, SPAWN_BELOW, SPAWN_ABOVE, SPAWN_SIZE_XZ);
	}

	private static java.util.Optional<BoundingBox> readSpawnZone(CompoundTag tag) {
		if (!tag.contains("WWSpawnZone")) {
			return java.util.Optional.empty();
		}
		CompoundTag zone = tag.getCompoundOrEmpty("WWSpawnZone");
		return java.util.Optional.of(new BoundingBox(
				zone.getIntOr("minX", 0),
				zone.getIntOr("minY", 0),
				zone.getIntOr("minZ", 0),
				zone.getIntOr("maxX", 0),
				zone.getIntOr("maxY", 0),
				zone.getIntOr("maxZ", 0)));
	}

	@Override
	public BoundingBox getSpawnZone() {
		if (this.spawnZone == null) {
			this.spawnZone = computeSpawnZone();
		}
		return this.spawnZone;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
		BoundingBox zone = getSpawnZone();
		CompoundTag zoneTag = new CompoundTag();
		zoneTag.putInt("minX", zone.minX());
		zoneTag.putInt("minY", zone.minY());
		zoneTag.putInt("minZ", zone.minZ());
		zoneTag.putInt("maxX", zone.maxX());
		zoneTag.putInt("maxY", zone.maxY());
		zoneTag.putInt("maxZ", zone.maxZ());
		tag.put("WWSpawnZone", zoneTag);
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
		// super resets placement BB to the template footprint — keep it that way.
		this.spawnZone = computeSpawnZone();
		clearFloodedAirBlocks(level, chunkBox);
		setChestLoot(level, chunkBox, random);
	}

	/**
	 * Only the piece's real rotation — clearing all four rotations punches air holes
	 * into the ocean where other orientations' air cells land.
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
