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
import net.minecraft.world.item.Items;
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConfig;
import waterworld.WaterworldConstants;

public final class SpawnGearHandler {
	private SpawnGearHandler() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!WaterworldConfig.INSTANCE.spawnGear) return;

			ServerPlayer player = handler.getPlayer();
			if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) return;

			server.execute(() -> giveSpawnGear(player));
		});
	}

	private static void giveSpawnGear(ServerPlayer player) {
		ServerLevel level = player.level();

		BlockPos waterPos = BoatSpawnHelper.findWaterSurface(level, player.blockPosition(), 15);
		if (waterPos == null) {
			waterPos = new BlockPos(player.getBlockX(), WaterworldConstants.SEA_LEVEL, player.getBlockZ());
		}

		double x = waterPos.getX() + 0.5;
		double y = waterPos.getY() + 1.0;
		double z = waterPos.getZ() + 0.5;

		AbstractChestBoat raft = (AbstractChestBoat) EntityTypes.BAMBOO_CHEST_RAFT.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (raft == null) {
			ProjectWaterworld.LOGGER.warn("Failed to create bamboo chest raft for spawn gear");
			return;
		}

		raft.setPos(x, y, z);
		raft.setYRot(level.getRandom().nextFloat() * 360.0f);

		raft.setItem(0, new ItemStack(Items.BAMBOO));
		raft.setItem(1, new ItemStack(Items.FISHING_ROD));

		level.addFreshEntity(raft);

		player.teleportTo(level, x, y, z, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
		player.startRiding(raft);

		ProjectWaterworld.LOGGER.info("Gave spawn gear to {}", player.getName().getString());
	}
}
