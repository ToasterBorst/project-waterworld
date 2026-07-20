package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.entity.monster.Witch;
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

import java.util.List;

public class WitchHutBoatPiece extends TemplateStructurePiece {

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "chests/witch_hut_boat"));

	public WitchHutBoatPiece(StructureTemplateManager manager, Identifier template,
			BlockPos pos, RandomSource random) {
		super(WaterworldStructures.WITCH_HUT_BOAT_PIECE, 0, manager, template,
				template.toString(), makeSettings(Rotation.getRandom(random)), pos);
	}

	public WitchHutBoatPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(WaterworldStructures.WITCH_HUT_BOAT_PIECE, tag, context.structureTemplateManager(),
				id -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElse(Rotation.NONE)));
		// Legacy saves may have re-saved Rot=NONE while the placed blocks are rotated
		// (pre-Rot chunks loaded once by a Rot-saving build). The saved rotation can't
		// be trusted for coverage, so always widen to the union of all rotations.
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
		spawnInitialMobs(level, chunkBox, random);
	}

	/**
	 * Finds all positions the template defines as air and ensures they are air
	 * in the world — prevents ocean water from persisting inside the structure.
	 * With IGNORE_WATERLOGGING, waterloggable blocks retain their NBT-saved state:
	 * exterior hull blocks stay waterlogged, interior blocks stay dry.
	 */
	private void clearFloodedAirBlocks(WorldGenLevel level, BoundingBox chunkBox) {
		List<StructureTemplate.StructureBlockInfo> airBlocks =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.AIR);

		for (StructureTemplate.StructureBlockInfo info : airBlocks) {
			BlockPos worldPos = info.pos();
			if (!chunkBox.isInside(worldPos)) continue;

			BlockState existing = level.getBlockState(worldPos);
			if (existing.is(Blocks.WATER)) {
				level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 2);
			}
		}

		List<StructureTemplate.StructureBlockInfo> caveAirBlocks =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.CAVE_AIR);

		for (StructureTemplate.StructureBlockInfo info : caveAirBlocks) {
			BlockPos worldPos = info.pos();
			if (!chunkBox.isInside(worldPos)) continue;

			BlockState existing = level.getBlockState(worldPos);
			if (existing.is(Blocks.WATER)) {
				level.setBlock(worldPos, Blocks.CAVE_AIR.defaultBlockState(), 2);
			}
		}
	}

	private void setChestLoot(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		List<StructureTemplate.StructureBlockInfo> chests =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.CHEST);

		for (StructureTemplate.StructureBlockInfo info : chests) {
			BlockPos pos = info.pos();
			if (!chunkBox.isInside(pos)) continue;
			if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
				container.setLootTable(LOOT_TABLE, random.nextLong());
			}
		}

		List<StructureTemplate.StructureBlockInfo> trappedChests =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.TRAPPED_CHEST);

		for (StructureTemplate.StructureBlockInfo info : trappedChests) {
			BlockPos pos = info.pos();
			if (!chunkBox.isInside(pos)) continue;
			if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
				container.setLootTable(LOOT_TABLE, random.nextLong());
			}
		}
	}

	private void spawnInitialMobs(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		if (!(level instanceof ServerLevelAccessor serverLevel)) return;

		BoundingBox bb = this.getBoundingBox();
		int centerX = (bb.minX() + bb.maxX()) / 2;
		int centerZ = (bb.minZ() + bb.maxZ()) / 2;
		int spawnY = bb.maxY() - 1;

		if (!chunkBox.isInside(new BlockPos(centerX, spawnY, centerZ))) return;

		try {
			Witch witch = EntityTypes.WITCH.create(serverLevel.getLevel(), EntitySpawnReason.STRUCTURE);
			if (witch != null) {
				witch.setPersistenceRequired();
				witch.snapTo(centerX + 0.5, spawnY, centerZ + 0.5,
						random.nextFloat() * 360.0F, 0.0F);
				serverLevel.addFreshEntityWithPassengers(witch);
			}

			spawnCatOnCraftingTable(serverLevel, level, chunkBox, random);
		} catch (Exception e) {
			ProjectWaterworld.LOGGER.warn("Failed to spawn initial witch hut mobs: {}", e.getMessage());
		}
	}

	private void spawnCatOnCraftingTable(ServerLevelAccessor serverLevel, WorldGenLevel level,
			BoundingBox chunkBox, RandomSource random) {
		List<StructureTemplate.StructureBlockInfo> tables =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.CRAFTING_TABLE);

		BlockPos tableTop = null;
		for (StructureTemplate.StructureBlockInfo info : tables) {
			if (chunkBox.isInside(info.pos())) {
				tableTop = info.pos().above();
				break;
			}
		}

		if (tableTop == null) {
			BoundingBox bb = this.getBoundingBox();
			tableTop = new BlockPos((bb.minX() + bb.maxX()) / 2 + 1, bb.maxY() - 1, (bb.minZ() + bb.maxZ()) / 2);
		}

		Cat cat = EntityTypes.CAT.create(serverLevel.getLevel(), EntitySpawnReason.STRUCTURE);
		if (cat != null) {
			cat.snapTo(tableTop.getX() + 0.5, tableTop.getY(), tableTop.getZ() + 0.5, 0.0F, 0.0F);
			cat.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(tableTop),
					EntitySpawnReason.STRUCTURE, null);

			Holder.Reference<CatVariant> allBlack = serverLevel.registryAccess()
					.lookupOrThrow(Registries.CAT_VARIANT)
					.get(CatVariants.ALL_BLACK)
					.orElse(null);
			if (allBlack != null) {
				cat.setVariant(allBlack);
			}

			cat.setPersistenceRequired();
			cat.setTame(true, false);
			cat.setOrderedToSit(true);
			cat.setInSittingPose(true);
			serverLevel.addFreshEntityWithPassengers(cat);
		}
	}
}
