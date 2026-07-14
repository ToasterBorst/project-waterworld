package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.illager.Pillager;
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
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConstants;

import java.util.List;

public class PillagerOutpostShipPiece extends TemplateStructurePiece {

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.withDefaultNamespace("chests/pillager_outpost"));

	private static final int INITIAL_PILLAGERS = 5;

	public PillagerOutpostShipPiece(StructureTemplateManager manager, Identifier template,
			BlockPos pos, RandomSource random) {
		super(WaterworldStructures.PILLAGER_OUTPOST_SHIP_PIECE, 0, manager, template,
				template.toString(), makeSettings(random), pos);
	}

	public PillagerOutpostShipPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(WaterworldStructures.PILLAGER_OUTPOST_SHIP_PIECE, tag, context.structureTemplateManager(),
				id -> makeSettings(null));
	}

	private static StructurePlaceSettings makeSettings(RandomSource random) {
		Rotation rotation = random != null
				? Rotation.getRandom(random)
				: Rotation.NONE;
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setKnownShape(true)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
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
		spawnInitialPillagers(level, chunkBox, random);
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

	private void spawnInitialPillagers(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		if (!(level instanceof ServerLevelAccessor serverLevel)) return;

		BoundingBox bb = this.getBoundingBox();
		int minDeckY = Math.max(bb.minY(), WaterworldConstants.SEA_LEVEL);

		try {
			boolean spawnedLeader = false;
			int spawned = 0;

			for (int attempt = 0; attempt < 40 && spawned < INITIAL_PILLAGERS; attempt++) {
				int x = bb.minX() + random.nextInt(Math.max(1, bb.getXSpan()));
				int z = bb.minZ() + random.nextInt(Math.max(1, bb.getZSpan()));

				BlockPos floor = findDeckFloor(level, x, z, minDeckY, bb.maxY());
				if (floor == null || !chunkBox.isInside(floor)) continue;

				Pillager pillager = EntityTypes.PILLAGER.create(
						serverLevel.getLevel(), EntitySpawnReason.STRUCTURE);
				if (pillager == null) continue;

				pillager.setPersistenceRequired();
				if (!spawnedLeader) {
					pillager.setPatrolLeader(true);
					spawnedLeader = true;
				}
				pillager.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(floor),
						EntitySpawnReason.STRUCTURE, null);
				pillager.snapTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5,
						random.nextFloat() * 360.0F, 0.0F);
				serverLevel.addFreshEntityWithPassengers(pillager);
				spawned++;
			}
		} catch (Exception e) {
			ProjectWaterworld.LOGGER.warn("Failed to spawn pillager outpost ship mobs: {}",
					e.getMessage());
		}
	}

	private static BlockPos findDeckFloor(WorldGenLevel level, int x, int z, int minY, int maxY) {
		for (int y = maxY; y >= minY; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (level.getBlockState(pos.below()).isSolidRender()
					&& level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.above()).isAir()) {
				return pos;
			}
		}
		return null;
	}
}
