package waterworld.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
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
import waterworld.WaterworldDetection;
import waterworld.spawn.BoatSpawnHelper;
import waterworld.spawn.MobEquipmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * After each raid wave spawns, puts raiders in boats.
 * Land-spawned raiders are relocated to nearby water.
 * Non-Ravager raiders are paired 2-per-boat.
 * Ravagers ride in seat 1 with a dedicated pilot; jockeys preserved.
 */
@Mixin(Raid.class)
public class RaidSpawnMixin {

	@Inject(method = "spawnGroup", at = @At("TAIL"))
	private void waterworld$mountRaidersInBoats(ServerLevel level, BlockPos pos, CallbackInfo ci) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.oceanPillagerPatrols) return;
		if (!WaterworldDetection.isActive()) return;

		Raid raid = (Raid) (Object) this;

		AABB area = new AABB(pos).inflate(32);
		List<Raider> raiders = level.getEntitiesOfClass(Raider.class, area,
				raider -> raider.getCurrentRaid() != null && !raider.isPassenger());

		List<Raider> ravagers = new ArrayList<>();
		List<Raider> infantry = new ArrayList<>();

		for (Raider raider : raiders) {
			moveToWaterIfNeeded(level, raider);

			MobEquipmentHelper.equipRandomArmorForRaid(raider, level.getDifficulty(),
					level.getRandom(), level.getGameTime(), raid.getGroupsSpawned());

			if (raider instanceof Ravager) {
				ravagers.add(raider);
			} else {
				infantry.add(raider);
			}
		}

		for (Raider ravager : ravagers) {
			mountRavagerBoat(level, (Ravager) ravager, infantry);
		}

		for (int i = 0; i < infantry.size(); i++) {
			Raider raider = infantry.get(i);
			if (raider.isPassenger()) continue;

			AbstractBoat boat = BoatSpawnHelper.spawnBoatAt(level,
					raider.getX(), raider.getY(), raider.getZ());
			if (boat == null) continue;

			raider.startRiding(boat);
			BoatSpawnHelper.addBoatAI(raider, true);

			Raider passenger = findUnmountedInfantry(infantry, i + 1);
			if (passenger != null) {
				passenger.snapTo(raider.getX(), raider.getY(), raider.getZ(),
						passenger.getYRot(), 0.0f);
				passenger.startRiding(boat);
				BoatSpawnHelper.addBoatAI(passenger, false);
			}
		}
	}

	private static void moveToWaterIfNeeded(ServerLevel level, Raider raider) {
		if (BoatSpawnHelper.isWaterSurface(level, raider.blockPosition())
				|| BoatSpawnHelper.isWaterSurface(level, raider.blockPosition().below())) {
			return;
		}
		BlockPos nearbyWater = BoatSpawnHelper.findWaterSurface(level, raider.blockPosition(), 16);
		if (nearbyWater == null) return;
		raider.snapTo(nearbyWater.getX() + 0.5, nearbyWater.getY() + 1.0,
				nearbyWater.getZ() + 0.5, raider.getYRot(), 0.0f);
	}

	private static void mountRavagerBoat(ServerLevel level, Ravager ravager,
			List<Raider> infantry) {
		Entity jockey = ravager.getFirstPassenger();

		Raider pilot = findUnmountedInfantry(infantry, 0);
		if (pilot == null) {
			pilot = createPilot(level, ravager);
		}
		if (pilot == null) return;

		AbstractBoat boat = BoatSpawnHelper.spawnBoatAt(level,
				ravager.getX(), ravager.getY(), ravager.getZ());
		if (boat == null) return;

		pilot.startRiding(boat);
		BoatSpawnHelper.addBoatAI(pilot, true);

		ravager.startRiding(boat);
		BoatSpawnHelper.addBoatAI(ravager, false);

		if (jockey instanceof Mob jockeyMob && !jockeyMob.isPassenger()) {
			jockeyMob.startRiding(ravager);
		}
	}

	private static Raider findUnmountedInfantry(List<Raider> infantry, int startIndex) {
		for (int i = startIndex; i < infantry.size(); i++) {
			Raider candidate = infantry.get(i);
			if (!candidate.isPassenger()) return candidate;
		}
		return null;
	}

	private static Pillager createPilot(ServerLevel level, Ravager ravager) {
		Pillager pilot = EntityTypes.PILLAGER.create(level, EntitySpawnReason.EVENT);
		if (pilot != null) {
			pilot.snapTo(ravager.getX(), ravager.getY(), ravager.getZ(), 0.0f, 0.0f);
			pilot.finalizeSpawn(level, level.getCurrentDifficultyAt(ravager.blockPosition()),
					EntitySpawnReason.EVENT, null);
			MobEquipmentHelper.equipRandomArmor(pilot, level.getDifficulty(),
					level.getRandom(), level.getGameTime());
			level.addFreshEntity(pilot);
		}
		return pilot;
	}
}
