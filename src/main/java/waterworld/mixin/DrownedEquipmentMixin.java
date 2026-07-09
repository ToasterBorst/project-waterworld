package waterworld.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

/**
 * Prevents naturally spawned drowned from receiving tridents
 * until the world has passed a configurable day threshold.
 */
@Mixin(Drowned.class)
public class DrownedEquipmentMixin {

	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void waterworld$stripEarlyTrident(net.minecraft.util.RandomSource random,
			DifficultyInstance difficulty, CallbackInfo ci) {
		if (!WaterworldDetection.isActive()) return;
		Drowned self = (Drowned) (Object) this;
		Level level = self.level();
		if (!(level instanceof ServerLevel serverLevel)) return;

		int minDays = WaterworldConfig.INSTANCE.tridentDrownedMinDays;
		if (minDays <= 0) return;

		if (serverLevel.getGameTime() < 24000L * minDays) {
			ItemStack mainHand = self.getItemBySlot(EquipmentSlot.MAINHAND);
			if (mainHand.is(Items.TRIDENT)) {
				self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			}
		}
	}
}
