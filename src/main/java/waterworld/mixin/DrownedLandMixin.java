package waterworld.mixin;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import waterworld.WaterworldConfig;
import waterworld.WaterworldDetection;

/**
 * Removes the DrownedGoToWaterGoal so drowned can roam on land,
 * and adds a RandomStrollGoal for active wandering.
 */
@Mixin(Drowned.class)
public class DrownedLandMixin {

	@Inject(method = "addBehaviourGoals", at = @At("TAIL"))
	private void waterworld$allowLandRoaming(CallbackInfo ci) {
		if (!WaterworldConfig.INSTANCE.drownedCanGoOnLand) return;
		if (!WaterworldDetection.isActive()) return;

		Drowned self = (Drowned) (Object) this;
		self.goalSelector.getAvailableGoals().removeIf(
			wrapped -> wrapped.getGoal() instanceof Drowned.DrownedGoToWaterGoal
		);
		self.goalSelector.addGoal(6, new RandomStrollGoal(self, 1.0));
	}
}
