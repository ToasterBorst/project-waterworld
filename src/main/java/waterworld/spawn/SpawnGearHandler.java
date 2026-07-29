package waterworld.spawn;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.ItemStack;
import waterworld.WaterworldMod;
import waterworld.WaterworldConfig;
import waterworld.WaterworldConstants;
import waterworld.WaterworldDetection;
import waterworld.compat.SpawnPartyBridge;

import java.util.List;

public final class SpawnGearHandler {
	private SpawnGearHandler() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!WaterworldDetection.isActive()) return;
			if (!WaterworldConfig.INSTANCE.spawnGear) return;

			ServerPlayer player = handler.getPlayer();
			if (SpawnGearTracker.hasReceived(player.getUUID())) return;
			if (SpawnPartyBridge.shouldDeferJoinGear(player.getUUID())) {
				WaterworldMod.LOGGER.info("Deferring spawn gear for {} until Spawn Party origin placement",
						player.getName().getString());
				return;
			}
			// Only without Spawn Party: legacy players with play time skip re-grant.
			if (!SpawnPartyBridge.isSpawnPartyPresent()
					&& player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) {
				SpawnGearTracker.markReceived(player.getUUID());
				return;
			}

			server.execute(() -> giveSpawnGearAt(player, player.blockPosition()));
		});
	}

	/** Public entry for Spawn Party placement (and JOIN when not deferred). */
	public static void giveSpawnGearAt(ServerPlayer player, BlockPos near) {
		if (!WaterworldConfig.INSTANCE.spawnGear) return;
		if (SpawnGearTracker.hasReceived(player.getUUID())) return;

		ServerLevel level = player.level();
		BlockPos waterPos = BoatSpawnHelper.findWaterSurface(level, near, 15);
		if (waterPos == null) {
			waterPos = new BlockPos(near.getX(), WaterworldConstants.seaLevel(), near.getZ());
		}

		double x = waterPos.getX() + 0.5;
		double y = waterPos.getY() + 1.0;
		double z = waterPos.getZ() + 0.5;

		AbstractChestBoat raft = (AbstractChestBoat) EntityTypes.BAMBOO_CHEST_RAFT.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (raft == null) {
			WaterworldMod.LOGGER.warn("Failed to create bamboo chest raft for spawn gear");
			return;
		}

		raft.setPos(x, y, z);
		raft.setYRot(level.getRandom().nextFloat() * 360.0f);

		List<ItemStack> gear = WaterworldConfig.INSTANCE.createSpawnGearStacks();
		int slots = raft.getContainerSize();
		for (int i = 0; i < gear.size() && i < slots; i++) {
			raft.setItem(i, gear.get(i));
		}

		level.addFreshEntity(raft);

		player.teleportTo(level, x, y, z, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
		player.startRiding(raft);

		SpawnGearTracker.markReceived(player.getUUID());
		WaterworldMod.LOGGER.info("Gave spawn gear to {} near {},{},{}",
				player.getName().getString(), waterPos.getX(), waterPos.getY(), waterPos.getZ());
	}
}
