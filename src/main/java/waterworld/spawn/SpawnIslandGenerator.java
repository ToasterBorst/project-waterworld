package waterworld.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import waterworld.WaterworldConstants;
import waterworld.worldgen.WaterworldBiomeTags;

/**
 * Generates a spawn island as 2-3 distinct steep peaks (flooded seamounts).
 * One peak barely breaches the surface; others remain 5-9 blocks underwater.
 * Peaks have irregular cross-sections via noise-warped distance, steep sides
 * that taper gently into the seafloor at depth.
 */
public final class SpawnIslandGenerator {
	private static final int SCAN_RADIUS = 48;

	private static final double DROP_COEFF = 0.7;
	private static final double DROP_POWER = 1.4;
	private static final double WARP_STRENGTH = 0.35;
	private static final double DETAIL_FREQ = 0.07;
	private static final double DETAIL_SCALE = 1.5;

	private static final long NOISE_SALT_WARP = 0x5A7EF1C3D2B48E07L;
	private static final long NOISE_SALT_DETAIL = 0x3B19E72A4C8D5F06L;

	private SpawnIslandGenerator() {
	}

	public static int generate(ServerLevel level, int centerX, int centerZ) {
		int seaLevel = WaterworldConstants.SEA_LEVEL;
		long seed = level.getSeed();

		SimplexNoise warpNoise = new SimplexNoise(RandomSource.create(seed ^ NOISE_SALT_WARP));
		SimplexNoise detailNoise = new SimplexNoise(RandomSource.create(seed ^ NOISE_SALT_DETAIL));

		Holder<Biome> biome = level.getBiome(new BlockPos(centerX, seaLevel - 10, centerZ));
		boolean isCold = biome.is(WaterworldBiomeTags.SPAWN_ISLAND_COLD);
		boolean isWarmOcean = biome.is(WaterworldBiomeTags.SPAWN_ISLAND_WARM);

		// Primary peak: barely breaches surface (1 block above water)
		double p0OffX = warpNoise.getValue(centerX * 0.001, centerZ * 0.001) * 4.0;
		double p0OffZ = warpNoise.getValue(centerX * 0.001 + 50, centerZ * 0.001 + 50) * 4.0;
		double[] peak0 = {centerX + p0OffX, centerZ + p0OffZ, seaLevel + 1};

		// Secondary peak: 5-7 blocks below surface, offset 8-14 blocks from primary
		double sp1Angle = warpNoise.getValue(centerX * 0.003 + 200, centerZ * 0.003 + 200) * Math.PI * 2;
		double sp1Dist = 8.0 + Math.abs(warpNoise.getValue(centerX * 0.004, centerZ * 0.004)) * 6.0;
		double sp1Y = seaLevel - 5 - Math.abs(detailNoise.getValue(centerX * 0.005, centerZ * 0.005)) * 2;
		double[] peak1 = {peak0[0] + Math.cos(sp1Angle) * sp1Dist, peak0[1] + Math.sin(sp1Angle) * sp1Dist, sp1Y};

		// Tertiary peak: 7-9 blocks below surface, offset at different angle
		double sp2Angle = sp1Angle + 2.0 + warpNoise.getValue(centerX * 0.003 + 400, centerZ * 0.003 + 400) * 0.6;
		double sp2Dist = 12.0 + Math.abs(warpNoise.getValue(centerX * 0.004 + 100, centerZ * 0.004 + 100)) * 6.0;
		double sp2Y = seaLevel - 7 - Math.abs(detailNoise.getValue(centerX * 0.005 + 50, centerZ * 0.005 + 50)) * 2;
		double[] peak2 = {peak0[0] + Math.cos(sp2Angle) * sp2Dist, peak0[1] + Math.sin(sp2Angle) * sp2Dist, sp2Y};

		double[][] peaks = {peak0, peak1, peak2};

		int highestY = (int) peak0[2];
		int highestX = (int) Math.round(peak0[0]);
		int highestZ = (int) Math.round(peak0[1]);

		for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
			for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
				int bx = centerX + dx;
				int bz = centerZ + dz;

				int existingFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, bx, bz);

				// Per-block noise warp for irregular cross-sections
				double warp1 = warpNoise.getValue(bx * 0.08, bz * 0.08);
				double warp2 = warpNoise.getValue(bx * 0.12 + 77, bz * 0.12 + 77);
				double warpMult = 1.0 + warp1 * WARP_STRENGTH + warp2 * WARP_STRENGTH * 0.5;

				// Find max height contribution from all peaks
				double bestHeight = Double.NEGATIVE_INFINITY;
				for (double[] peak : peaks) {
					double ddx = bx - peak[0];
					double ddz = bz - peak[1];
					double dist = Math.sqrt(ddx * ddx + ddz * ddz);
					double warpedDist = dist * warpMult;

					// Power curve: gentle near peak, accelerates with distance
					double drop = DROP_COEFF * Math.pow(warpedDist, DROP_POWER);
					double h = peak[2] - drop;
					if (h > bestHeight) bestHeight = h;
				}

				// Detail noise for surface roughness (only at depth, not near surface)
				double depthFactor = Math.max(0, Math.min(1.0, (seaLevel - 3 - bestHeight) / 10.0));
				double detail = fractalNoise(detailNoise, bx, bz) * DETAIL_SCALE * depthFactor;
				double rawHeight = bestHeight + detail;

				int targetTop = (int) Math.round(rawHeight);
				if (targetTop <= existingFloor) continue;
				if (targetTop > (int) peak0[2]) targetTop = (int) peak0[2];

				for (int y = existingFloor; y <= targetTop; y++) {
					placeBiomeBlock(level, new BlockPos(bx, y, bz), y, targetTop, seaLevel, isCold);
				}

				if (targetTop >= seaLevel) {
					for (int y = targetTop + 1; y <= targetTop + 3; y++) {
						BlockPos pos = new BlockPos(bx, y, bz);
						if (!level.getBlockState(pos).isAir()) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}
		}

		// Bamboo sapling on the peak
		BlockPos peakBlock = new BlockPos(highestX, highestY, highestZ);
		BlockState peakState = level.getBlockState(peakBlock);
		if (peakState.is(Blocks.GRASS_BLOCK) || peakState.is(Blocks.DIRT) || peakState.is(Blocks.SAND)) {
			level.setBlock(peakBlock.above(), Blocks.BAMBOO_SAPLING.defaultBlockState(), 2);
		}

		decorateUnderwater(level, centerX, centerZ, seaLevel, isWarmOcean, isCold, detailNoise);

		return highestY + 1;
	}

	// ==================== CORAL STRUCTURES (warm_ocean only) ====================

	private static void decorateUnderwater(ServerLevel level, int centerX, int centerZ,
			int seaLevel, boolean isWarmOcean, boolean isCold, SimplexNoise noise) {
		RandomSource rand = level.getRandom();

		for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
			for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
				int bx = centerX + dx;
				int bz = centerZ + dz;
				int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, bx, bz);

				if (surfaceY >= seaLevel - 5) continue;

				BlockPos surfBlock = new BlockPos(bx, surfaceY - 1, bz);
				BlockState surfState = level.getBlockState(surfBlock);
				if (!surfState.is(Blocks.SAND) && !surfState.is(Blocks.GRAVEL)) continue;

				BlockPos abovePos = new BlockPos(bx, surfaceY, bz);
				if (!level.getBlockState(abovePos).is(Blocks.WATER)) continue;

				if (isWarmOcean) {
					double cluster = noise.getValue(bx * 0.04, bz * 0.04);
					if (cluster > 0.35 && rand.nextFloat() < 0.08) {
						buildCoralStructure(level, abovePos, seaLevel, rand);
					} else if (cluster > 0.0 && rand.nextFloat() < 0.06) {
						level.setBlock(abovePos, Blocks.SEAGRASS.defaultBlockState(), 2);
					}
				} else if (isCold) {
					if (rand.nextFloat() < 0.03 && surfaceY < seaLevel - 10) {
						placeKelp(level, abovePos, seaLevel, rand);
					}
				} else {
					double val = noise.getValue(bx * 0.05, bz * 0.05);
					if (val < -0.3 && surfaceY < seaLevel - 10 && rand.nextFloat() < 0.05) {
						placeKelp(level, abovePos, seaLevel, rand);
					} else if (val > 0.2 && rand.nextFloat() < 0.05) {
						level.setBlock(abovePos, Blocks.SEAGRASS.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	private static void buildCoralStructure(ServerLevel level, BlockPos base, int seaLevel, RandomSource rand) {
		int type = rand.nextInt(10);
		if (type < 5) {
			buildCoralTree(level, base, seaLevel, rand);
		} else if (type < 8) {
			buildCoralClaw(level, base, seaLevel, rand);
		} else {
			buildCoralMushroom(level, base, seaLevel, rand);
		}
	}

	private static void buildCoralTree(ServerLevel level, BlockPos base, int seaLevel, RandomSource rand) {
		BlockState coralBlock = pickCoralBlock(rand);
		int trunkHeight = 1 + rand.nextInt(3);

		for (int y = 0; y < trunkHeight; y++) {
			BlockPos p = base.above(y);
			if (p.getY() >= seaLevel - 1 || !level.getBlockState(p).is(Blocks.WATER)) return;
			level.setBlock(p, coralBlock, 2);
		}

		BlockPos trunkTop = base.above(trunkHeight - 1);
		int branches = 2 + rand.nextInt(3);
		Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		shuffleDirections(dirs, rand);

		for (int b = 0; b < branches && b < 4; b++) {
			Direction dir = dirs[b];
			BlockPos branchStart = trunkTop.relative(dir).above();
			if (branchStart.getY() >= seaLevel - 1 || !level.getBlockState(branchStart).is(Blocks.WATER)) continue;
			level.setBlock(branchStart, coralBlock, 2);

			int branchHeight = 1 + rand.nextInt(4);
			BlockPos current = branchStart;
			for (int h = 0; h < branchHeight; h++) {
				current = current.above();
				if (current.getY() >= seaLevel - 1 || !level.getBlockState(current).is(Blocks.WATER)) break;
				level.setBlock(current, coralBlock, 2);
			}

			BlockPos fanPos = current.above();
			if (fanPos.getY() < seaLevel - 1 && level.getBlockState(fanPos).is(Blocks.WATER)) {
				level.setBlock(fanPos, pickCoralFan(rand), 2);
			}
		}
	}

	private static void buildCoralClaw(ServerLevel level, BlockPos base, int seaLevel, RandomSource rand) {
		BlockState coralBlock = pickCoralBlock(rand);
		Direction mainDir = Direction.Plane.HORIZONTAL.getRandomDirection(rand);

		int arms = 2 + rand.nextInt(2);
		for (int a = 0; a < arms; a++) {
			int sideOffset = a - arms / 2;
			Direction sideDir = mainDir.getClockWise();
			BlockPos armBase = base.relative(sideDir, sideOffset).above(a > 0 ? 1 : 0);

			int length = 2 + rand.nextInt(3);
			BlockPos current = armBase;
			for (int i = 0; i < length; i++) {
				if (current.getY() >= seaLevel - 1 || !level.getBlockState(current).is(Blocks.WATER)) break;
				level.setBlock(current, coralBlock, 2);
				if (rand.nextInt(3) == 0) {
					current = current.relative(mainDir).above();
				} else {
					current = current.relative(mainDir);
				}
			}

			if (current.getY() < seaLevel - 1 && level.getBlockState(current).is(Blocks.WATER)) {
				level.setBlock(current, pickCoralFan(rand), 2);
			}
		}
	}

	private static void buildCoralMushroom(ServerLevel level, BlockPos base, int seaLevel, RandomSource rand) {
		BlockState coralBlock = pickCoralBlock(rand);
		int stemHeight = 1 + rand.nextInt(2);

		for (int y = 0; y < stemHeight; y++) {
			BlockPos p = base.above(y);
			if (p.getY() >= seaLevel - 1 || !level.getBlockState(p).is(Blocks.WATER)) return;
			level.setBlock(p, coralBlock, 2);
		}

		BlockPos capCenter = base.above(stemHeight);
		int capRadius = 1 + rand.nextInt(2);
		for (int cdx = -capRadius; cdx <= capRadius; cdx++) {
			for (int cdz = -capRadius; cdz <= capRadius; cdz++) {
				if (Math.abs(cdx) + Math.abs(cdz) > capRadius + 1) continue;
				BlockPos capPos = capCenter.offset(cdx, 0, cdz);
				if (capPos.getY() >= seaLevel - 1 || !level.getBlockState(capPos).is(Blocks.WATER)) continue;
				level.setBlock(capPos, coralBlock, 2);
			}
		}

		for (int cdx = -capRadius; cdx <= capRadius; cdx++) {
			for (int cdz = -capRadius; cdz <= capRadius; cdz++) {
				if (Math.abs(cdx) + Math.abs(cdz) > capRadius) continue;
				BlockPos fanPos = capCenter.offset(cdx, 1, cdz);
				if (fanPos.getY() >= seaLevel - 1) continue;
				if (level.getBlockState(fanPos).is(Blocks.WATER) && rand.nextFloat() < 0.5) {
					level.setBlock(fanPos, pickCoralFan(rand), 2);
				}
			}
		}
	}

	// ==================== UTILITY ====================

	private static void shuffleDirections(Direction[] dirs, RandomSource rand) {
		for (int i = dirs.length - 1; i > 0; i--) {
			int j = rand.nextInt(i + 1);
			Direction tmp = dirs[i];
			dirs[i] = dirs[j];
			dirs[j] = tmp;
		}
	}

	private static void placeKelp(ServerLevel level, BlockPos base, int seaLevel, RandomSource rand) {
		int maxHeight = seaLevel - base.getY() - 3;
		if (maxHeight < 3) return;
		int height = 3 + rand.nextInt(Math.min(maxHeight, 8));
		for (int i = 0; i < height - 1; i++) {
			BlockPos pos = base.above(i);
			if (!level.getBlockState(pos).is(Blocks.WATER)) return;
			level.setBlock(pos, Blocks.KELP_PLANT.defaultBlockState(), 2);
		}
		BlockPos topPos = base.above(height - 1);
		if (level.getBlockState(topPos).is(Blocks.WATER)) {
			level.setBlock(topPos, Blocks.KELP.defaultBlockState(), 2);
		}
	}

	private static BlockState pickCoralBlock(RandomSource rand) {
		return switch (rand.nextInt(5)) {
			case 0 -> Blocks.TUBE_CORAL_BLOCK.defaultBlockState();
			case 1 -> Blocks.BRAIN_CORAL_BLOCK.defaultBlockState();
			case 2 -> Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState();
			case 3 -> Blocks.FIRE_CORAL_BLOCK.defaultBlockState();
			default -> Blocks.HORN_CORAL_BLOCK.defaultBlockState();
		};
	}

	private static BlockState pickCoralFan(RandomSource rand) {
		return switch (rand.nextInt(5)) {
			case 0 -> Blocks.TUBE_CORAL_FAN.defaultBlockState();
			case 1 -> Blocks.BRAIN_CORAL_FAN.defaultBlockState();
			case 2 -> Blocks.BUBBLE_CORAL_FAN.defaultBlockState();
			case 3 -> Blocks.FIRE_CORAL_FAN.defaultBlockState();
			default -> Blocks.HORN_CORAL_FAN.defaultBlockState();
		};
	}

	private static void placeBiomeBlock(ServerLevel level, BlockPos pos, int y, int columnTop, int seaLevel, boolean isCold) {
		BlockState block;
		if (y == columnTop && columnTop > seaLevel) {
			block = Blocks.GRASS_BLOCK.defaultBlockState();
		} else if (y >= columnTop - 1 && columnTop > seaLevel) {
			block = Blocks.DIRT.defaultBlockState();
		} else if (y == columnTop) {
			block = isCold ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
		} else if (y >= columnTop - 3) {
			block = isCold ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
		} else {
			block = Blocks.STONE.defaultBlockState();
		}
		level.setBlock(pos, block, 2);
	}

	private static double fractalNoise(SimplexNoise noise, int worldX, int worldZ) {
		double value = 0.0;
		double amplitude = 1.0;
		double frequency = DETAIL_FREQ;
		double totalAmplitude = 0.0;

		for (int i = 0; i < 3; i++) {
			value += amplitude * noise.getValue(worldX * frequency, worldZ * frequency);
			totalAmplitude += amplitude;
			frequency *= 2.0;
			amplitude *= 0.5;
		}

		return value / totalAmplitude;
	}
}
