package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldDetection;

import java.util.List;

/**
 * Periodic spawner that ensures a witch inhabits each nearby witch_hut_boat
 * structure. Bypasses the global monster mob cap which in a waterworld is
 * typically saturated by drowned. Mirrors the vanilla CatSpawner pattern.
 */
public class WitchHutBoatSpawner {

	private static final int TICK_INTERVAL = 200;
	private static final int SCAN_RADIUS_CHUNKS = 4;
	private static final int MAX_WITCHES_PER_STRUCTURE = 1;

	private int nextTick;

	public void tick(ServerLevel level) {
		if (!WaterworldDetection.isActive()) return;
		if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

		nextTick--;
		if (nextTick > 0) return;
		nextTick = TICK_INTERVAL;

		Structure witchHutBoat = level.registryAccess()
				.lookupOrThrow(Registries.STRUCTURE)
				.getValue(WaterworldStructures.WITCH_HUT_BOAT_KEY);
		if (witchHutBoat == null) return;

		for (Player player : level.players()) {
			if (player.isSpectator()) continue;

			StructureStart start = findNearbyWitchHut(level, player.blockPosition(), witchHutBoat);
			if (start == null) continue;

			if (trySpawnWitch(level, start)) return;
		}
	}

	private StructureStart findNearbyWitchHut(ServerLevel level, BlockPos playerPos, Structure structure) {
		int playerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
		int playerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());

		for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
			for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
				int cx = playerChunkX + dx;
				int cz = playerChunkZ + dz;

				if (!level.hasChunk(cx, cz)) continue;

				var refs = level.getChunk(cx, cz).getAllReferences();
				if (!refs.containsKey(structure)) continue;

				BlockPos center = new ChunkPos(cx, cz).getMiddleBlockPosition(playerPos.getY());
				StructureStart start = level.structureManager().getStructureAt(center, structure);
				if (start.isValid()) return start;

				for (int y = level.getSeaLevel() - 5; y <= level.getSeaLevel() + 10; y++) {
					BlockPos probe = new BlockPos(center.getX(), y, center.getZ());
					start = level.structureManager().getStructureAt(probe, structure);
					if (start.isValid()) return start;
				}
			}
		}
		return null;
	}

	private boolean trySpawnWitch(ServerLevel level, StructureStart start) {
		BoundingBox structureBB = start.getBoundingBox();
		AABB searchArea = new AABB(
				structureBB.minX(), structureBB.minY(), structureBB.minZ(),
				structureBB.maxX() + 1, structureBB.maxY() + 1, structureBB.maxZ() + 1);

		List<Witch> existingWitches = level.getEntitiesOfClass(Witch.class, searchArea);
		if (existingWitches.size() >= MAX_WITCHES_PER_STRUCTURE) return false;

		RandomSource random = level.getRandom();
		BlockPos spawnPos = findValidSpawnPos(level, start, random);
		if (spawnPos == null) return false;

		Witch witch = EntityTypes.WITCH.create(level, EntitySpawnReason.NATURAL);
		if (witch != null) {
			witch.setPersistenceRequired();
			witch.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
					EntitySpawnReason.NATURAL, null);
			witch.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
					random.nextFloat() * 360.0F, 0.0F);
			level.addFreshEntityWithPassengers(witch);
			ProjectWaterworld.LOGGER.debug("WitchHutBoatSpawner: spawned witch at {}", spawnPos);
			return true;
		}
		return false;
	}

	private BlockPos findValidSpawnPos(ServerLevel level, StructureStart start, RandomSource random) {
		for (StructurePiece piece : start.getPieces()) {
			BoundingBox bb = piece.getBoundingBox();

			for (int attempt = 0; attempt < 10; attempt++) {
				int x = bb.minX() + random.nextInt(bb.getXSpan());
				int z = bb.minZ() + random.nextInt(bb.getZSpan());

				for (int y = bb.maxY(); y >= bb.minY(); y--) {
					BlockPos pos = new BlockPos(x, y, z);

					if (level.getBlockState(pos.below()).isSolidRender()
							&& level.getBlockState(pos).isAir()
							&& level.getBlockState(pos.above()).isAir()) {
						return pos;
					}
				}
			}
		}
		return null;
	}
}
