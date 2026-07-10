package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
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
	private static final int SEARCH_RADIUS = 48;
	private static final int MAX_WITCHES_PER_STRUCTURE = 1;

	private int nextTick;

	public void tick(ServerLevel level) {
		if (!WaterworldDetection.isActive()) return;
		if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

		nextTick--;
		if (nextTick > 0) return;
		nextTick = TICK_INTERVAL;

		List<? extends Player> players = level.players();
		if (players.isEmpty()) return;

		RandomSource random = level.getRandom();
		Player player = players.get(random.nextInt(players.size()));
		BlockPos playerPos = player.blockPosition();

		int offsetX = (8 + random.nextInt(SEARCH_RADIUS)) * (random.nextBoolean() ? -1 : 1);
		int offsetZ = (8 + random.nextInt(SEARCH_RADIUS)) * (random.nextBoolean() ? -1 : 1);
		BlockPos checkPos = playerPos.offset(offsetX, 0, offsetZ);

		if (!level.hasChunksAt(checkPos.getX() - 2, checkPos.getZ() - 2, checkPos.getX() + 2, checkPos.getZ() + 2)) {
			return;
		}

		Structure witchHutBoat = level.registryAccess()
				.lookupOrThrow(Registries.STRUCTURE)
				.getValue(WaterworldStructures.WITCH_HUT_BOAT_KEY);
		if (witchHutBoat == null) return;

		StructureStart start = level.structureManager().getStructureAt(checkPos, witchHutBoat);
		if (!start.isValid()) return;

		BoundingBox structureBB = start.getBoundingBox();
		AABB searchArea = new AABB(
				structureBB.minX(), structureBB.minY(), structureBB.minZ(),
				structureBB.maxX() + 1, structureBB.maxY() + 1, structureBB.maxZ() + 1);

		List<Witch> existingWitches = level.getEntitiesOfClass(Witch.class, searchArea);
		if (existingWitches.size() >= MAX_WITCHES_PER_STRUCTURE) return;

		BlockPos spawnPos = findValidSpawnPos(level, start, random);
		if (spawnPos == null) return;

		Witch witch = EntityTypes.WITCH.create(level, EntitySpawnReason.NATURAL);
		if (witch != null) {
			witch.setPersistenceRequired();
			witch.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
					EntitySpawnReason.NATURAL, null);
			witch.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
					random.nextFloat() * 360.0F, 0.0F);
			level.addFreshEntityWithPassengers(witch);
			ProjectWaterworld.LOGGER.debug("WitchHutBoatSpawner: spawned witch at {}", spawnPos);
		}
	}

	private BlockPos findValidSpawnPos(ServerLevel level, StructureStart start, RandomSource random) {
		for (StructurePiece piece : start.getPieces()) {
			BoundingBox bb = piece.getBoundingBox();

			for (int attempt = 0; attempt < 10; attempt++) {
				int x = bb.minX() + random.nextInt(bb.getXSpan());
				int z = bb.minZ() + random.nextInt(bb.getZSpan());

				for (int y = bb.maxY(); y >= bb.minY(); y--) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockPos below = pos.below();

					if (level.getBlockState(below).isSolidRender()
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
