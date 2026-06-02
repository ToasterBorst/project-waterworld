package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConfig;
import waterworld.spawn.BoatSpawnHelper;
import waterworld.spawn.MobEquipmentHelper;

import java.util.List;

/**
 * Enables pillager patrols to spawn on water surfaces in boats.
 * Completely replaces vanilla patrol spawning when enabled.
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

		ci.cancel();

		if (!spawnEnemies) return;
		if (level.getDifficulty() == Difficulty.PEACEFUL) return;
		if (!level.getGameRules().get(GameRules.SPAWN_PATROLS)) return;

		--this.nextTick;
		if (this.nextTick > 0) return;
		this.nextTick = 12000 + level.getRandom().nextInt(1200);

		if (level.getGameTime() < 24000L * 5) return;
		if (level.getRandom().nextInt(5) != 0) return;

		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) return;

		ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
		if (player.isSpectator()) return;

		BlockPos waterPos = BoatSpawnHelper.findWaterSurface(level, player.blockPosition(), 48);
		if (waterPos == null) return;

		int count = 2 + level.getRandom().nextInt(3);

		for (int i = 0; i < count; i++) {
			BlockPos memberPos;
			if (i == 0) {
				memberPos = waterPos;
			} else {
				BlockPos offset = BoatSpawnHelper.findWaterSurface(level, waterPos, 8);
				memberPos = offset != null ? offset : waterPos;
			}

			Pillager pillager = EntityType.PILLAGER.create(level, EntitySpawnReason.PATROL);
			if (pillager == null) continue;

			pillager.snapTo(memberPos.getX() + 0.5, memberPos.getY() + 1.0,
					memberPos.getZ() + 0.5, 0.0f, 0.0f);

			if (i == 0) {
				pillager.setPatrolLeader(true);
			}
			pillager.setPatrolling(true);
			pillager.finalizeSpawn(level, level.getCurrentDifficultyAt(memberPos),
					EntitySpawnReason.PATROL, null);

			MobEquipmentHelper.equipRandomArmor(pillager, level.getDifficulty(), level.getRandom());

			level.addFreshEntity(pillager);
			BoatSpawnHelper.mountAsPilot(level, pillager);
		}
	}
}
