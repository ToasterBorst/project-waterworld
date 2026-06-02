package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.spawn.BoatSpawnHelper;

import java.util.List;

/**
 * Spawns wandering traders in boats at sea with one llama passenger
 * instead of on land with two leashed llamas.
 */
@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderMixin {

	@Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
	private void waterworld$oceanTraderSpawn(ServerLevel level,
			CallbackInfoReturnable<Boolean> cir) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.wanderingTraderBoats) return;

		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) return;

		ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
		if (player.isSpectator()) return;

		BlockPos waterPos = BoatSpawnHelper.findWaterSurface(level, player.blockPosition(), 48);
		if (waterPos == null) return;

		double x = waterPos.getX() + 0.5;
		double y = waterPos.getY() + 1.0;
		double z = waterPos.getZ() + 0.5;

		WanderingTrader trader = EntityType.WANDERING_TRADER.create(level, EntitySpawnReason.NATURAL);
		if (trader == null) return;

		trader.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
		trader.setDespawnDelay(48000);
		trader.setWanderTarget(player.blockPosition());

		level.addFreshEntity(trader);

		AbstractBoat boat = BoatSpawnHelper.spawnBoatAt(level, x, waterPos.getY(), z);
		if (boat != null) {
			trader.startRiding(boat);
			BoatSpawnHelper.addBoatAI(trader, true);

			TraderLlama llama = EntityType.TRADER_LLAMA.create(level, EntitySpawnReason.NATURAL);
			if (llama != null) {
				llama.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
				level.addFreshEntity(llama);
				llama.startRiding(boat);
				BoatSpawnHelper.addBoatAI(llama, false);
			}
		}

		cir.setReturnValue(true);
	}
}
