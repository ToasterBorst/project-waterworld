package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConfig;
import waterworld.spawn.BoatSpawnHelper;
import waterworld.spawn.MobEquipmentHelper;

import java.util.List;

/**
 * After each raid wave spawns, puts raiders in boats when over water.
 * Ravagers go in the back seat with their jockey preserved;
 * illagers pilot from seat 0.
 */
@Mixin(Raid.class)
public class RaidSpawnMixin {

	@Inject(method = "spawnGroup", at = @At("TAIL"))
	private void waterworld$mountRaidersInBoats(ServerLevel level, BlockPos pos, CallbackInfo ci) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.oceanPillagerPatrols) return;

		AABB area = new AABB(pos).inflate(32);
		List<Raider> raiders = level.getEntitiesOfClass(Raider.class, area,
				raider -> raider.getCurrentRaid() != null && !raider.isPassenger());

		for (Raider raider : raiders) {
			if (!BoatSpawnHelper.isWaterSurface(level, raider.blockPosition())
					&& !BoatSpawnHelper.isWaterSurface(level, raider.blockPosition().below())) {
				continue;
			}

			Raid raid = (Raid) (Object) this;
			MobEquipmentHelper.equipRandomArmorForRaid(raider, level.getDifficulty(),
					level.getRandom(), raid.getGroupsSpawned());

			if (raider instanceof Ravager ravager) {
				Entity jockey = ravager.getFirstPassenger();

				Raider pilot = findOrCreatePilot(level, ravager, raiders);
				if (pilot != null) {
					AbstractBoat boat = BoatSpawnHelper.spawnBoatAt(level,
							ravager.getX(), ravager.getY(), ravager.getZ());
					if (boat != null) {
						pilot.startRiding(boat);
						BoatSpawnHelper.addBoatAI(pilot, true);

						ravager.startRiding(boat);
						BoatSpawnHelper.addBoatAI(ravager, false);

						if (jockey instanceof Mob jockeyMob && !jockeyMob.isPassenger()) {
							jockeyMob.startRiding(ravager);
						}
					}
				}
			} else {
				BoatSpawnHelper.mountAsPilot(level, raider);
			}
		}
	}

	private static Raider findOrCreatePilot(ServerLevel level, Ravager ravager,
			List<Raider> raiders) {
		for (Raider candidate : raiders) {
			if (candidate == ravager) continue;
			if (candidate instanceof Ravager) continue;
			if (candidate.isPassenger()) continue;
			return candidate;
		}

		Pillager pilot = EntityType.PILLAGER.create(level, EntitySpawnReason.EVENT);
		if (pilot != null) {
			pilot.snapTo(ravager.getX(), ravager.getY(), ravager.getZ(), 0.0f, 0.0f);
			pilot.finalizeSpawn(level, level.getCurrentDifficultyAt(ravager.blockPosition()),
					EntitySpawnReason.EVENT, null);
			level.addFreshEntity(pilot);
		}
		return pilot;
	}
}
