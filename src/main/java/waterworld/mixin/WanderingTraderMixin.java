package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;
import waterworld.spawn.BoatSpawnHelper;

import java.util.List;

/**
 * Spawns wandering traders in boats at sea with two llama passengers
 * (one riding the other) instead of on land with two leashed llamas.
 * When active, fully replaces vanilla trader spawns (no boatless fall-through).
 */
@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderMixin {

	@Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
	private void waterworld$oceanTraderSpawn(ServerLevel level,
			CallbackInfoReturnable<Boolean> cir) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.wanderingTraderBoats) return;
		if (!WaterworldDetection.isActive()) return;

		// From here on we own the spawn result — never fall through to vanilla land traders.
		double scaleFactor = WaterworldConfig.dayScaleFactor(
				level.getGameTime(), config.wanderingTraderMinDays, config.wanderingTraderFullStrengthDays);

		if (scaleFactor <= 0.0) {
			cir.setReturnValue(false);
			return;
		}

		if (scaleFactor < 1.0 && level.getRandom().nextDouble() > scaleFactor) {
			cir.setReturnValue(false);
			return;
		}

		// Vanilla parity: most spawn ticks do nothing.
		if (level.getRandom().nextInt(10) != 0) {
			cir.setReturnValue(false);
			return;
		}

		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			cir.setReturnValue(false);
			return;
		}

		ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
		if (player.isSpectator()) {
			cir.setReturnValue(false);
			return;
		}

		BlockPos waterPos = BoatSpawnHelper.findWaterSurface(level, player.blockPosition(), 48);
		if (waterPos == null) {
			cir.setReturnValue(false);
			return;
		}

		double x = waterPos.getX() + 0.5;
		double y = waterPos.getY() + 1.0;
		double z = waterPos.getZ() + 0.5;

		WanderingTrader trader = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.NATURAL);
		if (trader == null) {
			cir.setReturnValue(false);
			return;
		}

		trader.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
		trader.setDespawnDelay(48000);
		trader.setWanderTarget(player.blockPosition());
		level.addFreshEntity(trader);

		AbstractBoat boat = BoatSpawnHelper.spawnBoatAt(level, x, waterPos.getY(), z);
		if (boat == null || !trader.startRiding(boat)) {
			trader.discard();
			if (boat != null) {
				boat.discard();
			}
			cir.setReturnValue(false);
			return;
		}

		BoatSpawnHelper.addBoatAI(trader, true);

		TraderLlama llama1 = spawnTraderLlama(level, x, y, z, Llama.Variant.WHITE);
		if (llama1 != null) {
			llama1.startRiding(boat);

			TraderLlama llama2 = spawnTraderLlama(level, x, y, z, Llama.Variant.BROWN);
			if (llama2 != null) {
				llama2.startRiding(llama1);
			}
		}

		cir.setReturnValue(true);
	}

	private static TraderLlama spawnTraderLlama(ServerLevel level,
			double x, double y, double z, Llama.Variant variant) {
		TraderLlama llama = EntityTypes.TRADER_LLAMA.create(level, EntitySpawnReason.NATURAL);
		if (llama == null) return null;
		llama.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
		llama.setVariant(variant);
		level.addFreshEntity(llama);
		return llama;
	}
}
