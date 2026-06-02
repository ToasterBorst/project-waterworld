package waterworld.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import waterworld.WaterworldConfig;

/**
 * Allows bamboo to substitute for sticks in any recipe ingredient.
 * When an ingredient rejects bamboo, we re-test with a stick --
 * if the stick would have been accepted, bamboo is accepted too.
 *
 * Recursion-safe: the re-test uses a stick ItemStack, so the guard
 * on Items.BAMBOO exits immediately on the second entry.
 */
@Mixin(Ingredient.class)
public class BambooStickIngredientMixin {

	@Unique
	private static ItemStack waterworld$stickStack;

	@Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z",
			at = @At("RETURN"), cancellable = true)
	private void waterworld$bambooAsStick(ItemStack stack,
			CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) return;
		if (!WaterworldConfig.INSTANCE.bambooReplacesSticks) return;
		if (!stack.is(Items.BAMBOO)) return;

		if (waterworld$stickStack == null) {
			waterworld$stickStack = new ItemStack(Items.STICK);
		}

		if (((Ingredient) (Object) this).test(waterworld$stickStack)) {
			cir.setReturnValue(true);
		}
	}
}
