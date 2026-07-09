package waterworld.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;

/**
 * Attaches drowned riders to naturally spawned guardians.
 * Targets Mob.finalizeSpawn since Guardian doesn't override it.
 */
@Mixin(Mob.class)
public class DrownedRiderMixin {

	@Inject(method = "finalizeSpawn", at = @At("RETURN"))
	private void waterworld$attachDrownedRider(ServerLevelAccessor levelAccessor,
			DifficultyInstance difficulty, EntitySpawnReason spawnReason,
			SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
		if (!((Object) this instanceof Guardian guardian)) return;
		if (!WaterworldConfig.INSTANCE.drownedRideGuardians) return;
		if (spawnReason != EntitySpawnReason.NATURAL) return;
		if (!(levelAccessor.getLevel() instanceof ServerLevel level)) return;
		if (level.getRandom().nextDouble() > WaterworldConfig.INSTANCE.drownedRiderChance) return;

		Drowned drowned = EntityTypes.DROWNED.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (drowned == null) return;

		drowned.snapTo(guardian.getX(), guardian.getY(), guardian.getZ(), guardian.getYRot(), 0.0f);

		int minDays = WaterworldConfig.INSTANCE.tridentDrownedMinDays;
		boolean worldOldEnough = minDays <= 0 || level.getGameTime() >= 24000L * minDays;
		if (worldOldEnough && level.getRandom().nextDouble() < WaterworldConfig.INSTANCE.tridentRiderChance) {
			drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
			drowned.setDropChance(EquipmentSlot.MAINHAND, 0.08f);
		}

		level.addFreshEntity(drowned);
		drowned.startRiding(guardian);
	}
}
