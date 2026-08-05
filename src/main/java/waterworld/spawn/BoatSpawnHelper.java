package waterworld.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import waterworld.WaterworldAttachments;
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
		WaterworldAttachments.markSpawnBoat(boat);
		level.addFreshEntity(boat);
		return boat;
	}

	/**
	 * Adds mount/dismount/swim boat goals. Safe to call from ENTITY_LOAD.
	 */
	public static void addBaseBoatGoals(Mob mob) {
		if (hasBaseBoatGoals(mob)) return;

		if (WaterworldMobTypes.canDismountBoats(mob)) {
			mob.goalSelector.addGoal(1, new DismountBoatGoal(mob));
		}

		if (WaterworldMobTypes.canDismountBoats(mob) || WaterworldMobTypes.canPilotBoats(mob)) {
			mob.goalSelector.addGoal(2, new MountNearbyBoatGoal(mob));
			mob.goalSelector.addGoal(3, new SwimToLandGoal(mob));
		}
	}

	/**
	 * Adds steering goals for boat pilots. Call from spawn sites with the correct role.
	 */
	public static void addPilotBoatGoals(Mob mob) {
		if (!WaterworldMobTypes.canPilotBoats(mob)) return;
		if (hasPilotBoatGoals(mob)) return;

		if (WaterworldMobTypes.isHostileBoatPilot(mob)) {
			mob.goalSelector.addGoal(0, new BoatCombatPilotGoal(mob));
			mob.goalSelector.addGoal(1, new PilotBoatGoal(mob));
		} else if (mob instanceof WanderingTrader) {
			mob.goalSelector.addGoal(0, new BoatFleeGoal(mob));
			mob.goalSelector.addGoal(1, new BoatApproachPlayerGoal(mob));
			mob.goalSelector.addGoal(2, new PilotBoatGoal(mob));
		}
	}

	/**
	 * Adds boat AI goals to a mob, selecting role-specific goals based on type.
	 *
	 * @param canPilot if true, adds piloting goals (for illagers/traders);
	 *                 if false, only adds base mount/dismount goals
	 */
	public static void addBoatAI(Mob mob, boolean canPilot) {
		addBaseBoatGoals(mob);
		if (canPilot) {
			addPilotBoatGoals(mob);
		}
	}

	private static boolean hasBaseBoatGoals(Mob mob) {
		for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
			if (wg.getGoal() instanceof MountNearbyBoatGoal) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasPilotBoatGoals(Mob mob) {
		for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
			var goal = wg.getGoal();
			if (goal instanceof PilotBoatGoal
					|| goal instanceof BoatApproachPlayerGoal
					|| goal instanceof BoatFleeGoal
					|| goal instanceof BoatCombatPilotGoal) {
				return true;
			}
		}
		return false;
	}
}
