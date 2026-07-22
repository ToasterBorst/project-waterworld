package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.spawn.BoatSpawnHelper;
import waterworld.spawn.MobEquipmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables pillager patrols to spawn on water surfaces in boats.
 * Pairs pillagers 2-per-boat. Completely replaces vanilla patrol spawning.
 * Spawn frequency scales with world age (day-based difficulty ramp).
 */
@Mixin(PatrolSpawner.class)
public class PatrolSpawnerMixin {

	@Shadow
	private int nextTick;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void waterworld$oceanPatrolSpawn(ServerLevel level, boolean spawnEnemies,
			CallbackInfo ci) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.oceanPillagerPatrols) return;
		if (!WaterworldDetection.isActive()) return;

		ci.cancel();

		if (!spawnEnemies) return;
		if (level.getDifficulty() == Difficulty.PEACEFUL) return;
		if (!level.getGameRules().get(GameRules.SPAWN_PATROLS)) return;

		--this.nextTick;
		if (this.nextTick > 0) return;
		this.nextTick = 12000 + level.getRandom().nextInt(1200);

		double scaleFactor = WaterworldConfig.dayScaleFactor(
				level.getGameTime(), config.patrolMinDays, config.patrolFullStrengthDays);

		if (scaleFactor <= 0.0) return;

		// At low scale factors, skip most attempts (makes early-game patrols rare)
		if (scaleFactor < 1.0 && level.getRandom().nextDouble() > scaleFactor) return;

		// Additional random gate to reduce overall frequency
		if (level.getRandom().nextInt(5) != 0) return;

		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) return;

		ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
		if (player.isSpectator()) return;

		BlockPos waterPos = BoatSpawnHelper.findWaterSurface(level, player.blockPosition(), 48);
		if (waterPos == null) return;

		int count = 2 + level.getRandom().nextInt(3);
		List<Pillager> spawned = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			BlockPos memberPos;
			if (i == 0) {
				memberPos = waterPos;
			} else {
				BlockPos offset = BoatSpawnHelper.findWaterSurface(level, waterPos, 8);
				memberPos = offset != null ? offset : waterPos;
			}

			Pillager pillager = EntityTypes.PILLAGER.create(level, EntitySpawnReason.PATROL);
			if (pillager == null) continue;

			pillager.snapTo(memberPos.getX() + 0.5, memberPos.getY() + 1.0,
					memberPos.getZ() + 0.5, 0.0f, 0.0f);

			if (i == 0) {
				pillager.setPatrolLeader(true);
			}
			pillager.setPatrolling(true);
			pillager.finalizeSpawn(level, level.getCurrentDifficultyAt(memberPos),
					EntitySpawnReason.PATROL, null);

			MobEquipmentHelper.equipRandomArmor(pillager, level.getDifficulty(),
					level.getRandom(), level.getGameTime());

			level.addFreshEntity(pillager);
			spawned.add(pillager);
		}

		for (int i = 0; i < spawned.size(); i += 2) {
			Pillager pilot = spawned.get(i);
			AbstractBoat boat = BoatSpawnHelper.spawnBoatAt(level,
					pilot.getX(), pilot.getY(), pilot.getZ());
			if (boat == null) continue;

			pilot.startRiding(boat);
			BoatSpawnHelper.addBoatAI(pilot, true);

			if (i + 1 < spawned.size()) {
				Pillager passenger = spawned.get(i + 1);
				passenger.snapTo(pilot.getX(), pilot.getY(), pilot.getZ(),
						passenger.getYRot(), 0.0f);
				passenger.startRiding(boat);
				BoatSpawnHelper.addBoatAI(passenger, false);
			}
		}
	}
}
