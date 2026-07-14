package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConstants;
import waterworld.WaterworldDetection;

import java.util.List;

/**
 * Periodic spawner keeping pillagers on nearby outpost ships. Follows the
 * witch hut boat / vanilla CatSpawner pattern and bypasses the drowned-saturated
 * monster mob cap.
 */
public class PillagerOutpostShipSpawner {

	private static final int TICK_INTERVAL = 200;
	private static final int MAX_PILLAGERS_PER_STRUCTURE = 5;

	private int nextTick;

	public void tick(ServerLevel level) {
		if (!WaterworldDetection.isActive()) return;

		nextTick--;
		if (nextTick > 0) return;
		nextTick = TICK_INTERVAL;

		Player player = level.getRandomPlayer();
		if (player == null) return;

		Structure structure = level.registryAccess()
				.lookupOrThrow(Registries.STRUCTURE)
				.getValue(WaterworldStructures.PILLAGER_OUTPOST_KEY);
		if (structure == null) return;

		BlockPos playerPos = player.blockPosition();

		StructureStart start = level.structureManager()
				.getStructureWithPieceAt(playerPos, structure);

		if (!start.isValid()) {
			RandomSource random = level.getRandom();
			int offsetX = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
			int offsetZ = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
			BlockPos probe = playerPos.offset(offsetX, 0, offsetZ);

			if (!level.hasChunksAt(probe.getX() - 2, probe.getZ() - 2, probe.getX() + 2, probe.getZ() + 2))
				return;

			start = level.structureManager().getStructureWithPieceAt(probe, structure);
		}

		if (!start.isValid()) return;
		trySpawnPillager(level, start);
	}

	private void trySpawnPillager(ServerLevel level, StructureStart start) {
		BoundingBox structureBB = start.getBoundingBox();
		AABB searchArea = new AABB(
				structureBB.minX(), structureBB.minY(), structureBB.minZ(),
				structureBB.maxX() + 1, structureBB.maxY() + 1, structureBB.maxZ() + 1);

		List<Pillager> existing = level.getEntitiesOfClass(Pillager.class, searchArea);
		if (existing.size() >= MAX_PILLAGERS_PER_STRUCTURE) return;

		RandomSource random = level.getRandom();
		BlockPos spawnPos = findValidSpawnPos(level, start, random);
		if (spawnPos == null) return;

		Pillager pillager = EntityTypes.PILLAGER.create(level, EntitySpawnReason.NATURAL);
		if (pillager != null) {
			pillager.setPersistenceRequired();
			pillager.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
					EntitySpawnReason.NATURAL, null);
			pillager.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
					random.nextFloat() * 360.0F, 0.0F);
			level.addFreshEntityWithPassengers(pillager);
			ProjectWaterworld.LOGGER.debug("PillagerOutpostShipSpawner: spawned pillager at {}",
					spawnPos);
		}
	}

	private BlockPos findValidSpawnPos(ServerLevel level, StructureStart start, RandomSource random) {
		for (StructurePiece piece : start.getPieces()) {
			BoundingBox bb = piece.getBoundingBox();
			int minDeckY = Math.max(bb.minY(), WaterworldConstants.SEA_LEVEL);

			for (int attempt = 0; attempt < 5; attempt++) {
				int x = bb.minX() + random.nextInt(Math.max(1, bb.getXSpan()));
				int z = bb.minZ() + random.nextInt(Math.max(1, bb.getZSpan()));

				for (int y = bb.maxY(); y >= minDeckY; y--) {
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
