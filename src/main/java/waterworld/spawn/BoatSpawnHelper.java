package waterworld.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import waterworld.WaterworldConfig;
import waterworld.ai.BoatApproachPlayerGoal;
import waterworld.ai.BoatCombatPilotGoal;
import waterworld.ai.BoatFleeGoal;
import waterworld.ai.DismountBoatGoal;
import waterworld.ai.MountNearbyBoatGoal;
import waterworld.ai.PilotBoatGoal;
import waterworld.ai.SwimToLandGoal;

public final class BoatSpawnHelper {
	private BoatSpawnHelper() {
	}

	@Nullable
	public static BlockPos findWaterSurface(ServerLevel level, BlockPos center, int range) {
		for (int attempt = 0; attempt < 10; attempt++) {
			int dx = level.getRandom().nextInt(range * 2 + 1) - range;
			int dz = level.getRandom().nextInt(range * 2 + 1) - range;
			BlockPos columnPos = center.offset(dx, 0, dz);
			BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, columnPos);

			if (isWaterSurface(level, surfacePos.below())) {
				return surfacePos.below();
			}
			if (isWaterSurface(level, surfacePos)) {
				return surfacePos;
			}
		}
		return null;
	}

	/**
	 * Returns true if the block at pos is a water source with air above.
	 */
	public static boolean isWaterSurface(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		BlockState above = level.getBlockState(pos.above());
		return state.getFluidState().isSource() && above.isAir();
	}

	/**
	 * Spawns a bamboo raft at the given water-surface position and returns it.
	 */
	@Nullable
	public static AbstractBoat spawnBoatAt(ServerLevel level, double x, double y, double z) {
		AbstractBoat boat = EntityTypes.BAMBOO_RAFT.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (boat == null) return null;
		boat.setPos(x, y + 0.5, z);
		boat.setYRot(level.getRandom().nextFloat() * 360.0f);
		level.addFreshEntity(boat);
		return boat;
	}

	/**
	 * Adds boat AI goals to a mob, selecting role-specific goals based on type.
	 * Skips addition if goals are already present (prevents accumulation from
	 * repeated ENTITY_LOAD events).
	 *
	 * @param canPilot if true, adds piloting goals (for illagers/traders);
	 *                 if false, only adds DismountBoatGoal (for ravagers/llamas)
	 */
	public static void addBoatAI(Mob mob, boolean canPilot) {
		if (hasBoatGoals(mob)) return;

		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (config.mobsCanExitBoats) {
			mob.goalSelector.addGoal(1, new DismountBoatGoal(mob));
		}

		mob.goalSelector.addGoal(2, new MountNearbyBoatGoal(mob));
		mob.goalSelector.addGoal(3, new SwimToLandGoal(mob));

		if (!canPilot || !config.mobsCanPilotBoats) return;

		if (WaterworldMobTypes.isHostileBoatPilot(mob)) {
			mob.goalSelector.addGoal(0, new BoatCombatPilotGoal(mob));
			mob.goalSelector.addGoal(1, new PilotBoatGoal(mob));
		} else if (mob instanceof WanderingTrader) {
			mob.goalSelector.addGoal(0, new BoatFleeGoal(mob));
			mob.goalSelector.addGoal(1, new BoatApproachPlayerGoal(mob));
			mob.goalSelector.addGoal(2, new PilotBoatGoal(mob));
		} else {
			mob.goalSelector.addGoal(0, new PilotBoatGoal(mob));
		}
	}

	private static boolean hasBoatGoals(Mob mob) {
		return mob.goalSelector.getAvailableGoals().stream()
				.anyMatch(wg -> wg.getGoal() instanceof DismountBoatGoal
						|| wg.getGoal() instanceof PilotBoatGoal
						|| wg.getGoal() instanceof MountNearbyBoatGoal);
	}
}
