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

/**
 * Witch hut boat piece. Placement BB is the full rotated template (multi-chunk safe).
 * Natural witch/cat overrides use a separate vanilla-sized {@code 7×7×9} cabin spawn zone.
 */
public class WitchHutBoatPiece extends TemplateStructurePiece implements SpawnZonePiece {

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.fromNamespaceAndPath(ProjectWaterworld.MOD_ID, "chests/witch_hut_boat"));

	/** Local crafting-table position (cabin floor). */
	private static final BlockPos CABIN_ANCHOR_LOCAL = new BlockPos(4, 5, 18);

	/**
	 * Vanilla swamp hut piece is 7×7×9. Cabin floor is local Y=5; zone runs from
	 * one below the floor through the cabin volume (Y 4–10), full template width,
	 * and 9 blocks of depth centered on the cabin.
	 */
	private static final int ZONE_MIN_X = 0;
	private static final int ZONE_MAX_X = 6;
	private static final int ZONE_MIN_Y = 4;
	private static final int ZONE_MAX_Y = 10;
	private static final int ZONE_MIN_Z = 14;
	private static final int ZONE_MAX_Z = 22;

	private BoundingBox spawnZone;
	private boolean spawnedWitch;
	private boolean spawnedCat;

	public WitchHutBoatPiece(StructureTemplateManager manager, Identifier template,
			BlockPos pos, RandomSource random) {
		super(WaterworldStructures.WITCH_HUT_BOAT_PIECE, 0, manager, template,
				template.toString(), makeSettings(Rotation.getRandom(random)), pos);
		this.spawnZone = computeSpawnZone();
	}

	public WitchHutBoatPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(WaterworldStructures.WITCH_HUT_BOAT_PIECE, tag, context.structureTemplateManager(),
				id -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElse(Rotation.NONE)));
		this.spawnedWitch = tag.getBooleanOr("Witch", false);
		this.spawnedCat = tag.getBooleanOr("Cat", false);
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
		return StructureSpawnZones.localBoxToWorld(
				this.templatePosition, this.placeSettings.getRotation(),
				ZONE_MIN_X, ZONE_MIN_Y, ZONE_MIN_Z, ZONE_MAX_X, ZONE_MAX_Y, ZONE_MAX_Z);
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
		tag.putBoolean("Witch", this.spawnedWitch);
		tag.putBoolean("Cat", this.spawnedCat);
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
		spawnInitialMobs(level, chunkBox, random);
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

	private void spawnInitialMobs(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
		if (!(level instanceof ServerLevelAccessor serverLevel)) return;

		BlockPos cabin = StructureSpawnZones.localToWorld(
				this.templatePosition, this.placeSettings.getRotation(), CABIN_ANCHOR_LOCAL);

		if (!this.spawnedWitch && chunkBox.isInside(cabin)) {
			this.spawnedWitch = true;
			try {
				Witch witch = EntityTypes.WITCH.create(serverLevel.getLevel(), EntitySpawnReason.STRUCTURE);
				if (witch != null) {
					witch.setPersistenceRequired();
					witch.snapTo(cabin.getX() + 0.5, cabin.getY(), cabin.getZ() + 0.5,
							random.nextFloat() * 360.0F, 0.0F);
					witch.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(cabin),
							EntitySpawnReason.STRUCTURE, null);
					serverLevel.addFreshEntityWithPassengers(witch);
				}
			} catch (Exception e) {
				ProjectWaterworld.LOGGER.warn("Failed to spawn witch hut witch: {}", e.getMessage());
			}
		}

		if (!this.spawnedCat) {
			spawnCatOnCraftingTable(serverLevel, level, chunkBox, random);
		}
	}

	private void spawnCatOnCraftingTable(ServerLevelAccessor serverLevel, WorldGenLevel level,
			BoundingBox chunkBox, RandomSource random) {
		BlockPos tableTop = null;
		List<StructureTemplate.StructureBlockInfo> tables =
				this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.CRAFTING_TABLE);
		for (StructureTemplate.StructureBlockInfo info : tables) {
			if (chunkBox.isInside(info.pos())) {
				tableTop = info.pos().above();
				break;
			}
		}

		if (tableTop == null) {
			BlockPos cabin = StructureSpawnZones.localToWorld(
					this.templatePosition, this.placeSettings.getRotation(), CABIN_ANCHOR_LOCAL);
			if (!chunkBox.isInside(cabin)) return;
			tableTop = cabin.above();
		}

		this.spawnedCat = true;
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
