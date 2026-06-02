package waterworld.spawn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import waterworld.WaterworldConfig;

public final class MobEquipmentHelper {
	private static final Item[] HELMETS = {
		Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET,
		Items.IRON_HELMET, Items.DIAMOND_HELMET
	};
	private static final Item[] CHESTPLATES = {
		Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE,
		Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE
	};
	private static final Item[] LEGGINGS = {
		Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS,
		Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS
	};
	private static final Item[] BOOTS = {
		Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS,
		Items.IRON_BOOTS, Items.DIAMOND_BOOTS
	};

	private static final float DROP_CHANCE = 0.05f;

	private MobEquipmentHelper() {
	}

	public static void equipRandomArmor(Mob mob, Difficulty difficulty, RandomSource random) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.pillagerArmor) return;

		float difficultyMultiplier = config.armorScalesWithDifficulty
				? getDifficultyMultiplier(difficulty)
				: 1.0f;

		double baseChance = config.pillagerArmorChance * difficultyMultiplier;
		int maxTier = getMaxTier(difficulty, config.armorScalesWithDifficulty);

		tryEquipSlot(mob, EquipmentSlot.HEAD, HELMETS, maxTier, baseChance, random);
		tryEquipSlot(mob, EquipmentSlot.CHEST, CHESTPLATES, maxTier, baseChance * 0.8, random);
		tryEquipSlot(mob, EquipmentSlot.LEGS, LEGGINGS, maxTier, baseChance * 0.6, random);
		tryEquipSlot(mob, EquipmentSlot.FEET, BOOTS, maxTier, baseChance * 0.7, random);
	}

	/**
	 * Higher chance variant for raid waves.
	 */
	public static void equipRandomArmorForRaid(Mob mob, Difficulty difficulty, RandomSource random, int wave) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.pillagerArmor) return;

		float waveBonus = 1.0f + (wave * 0.1f);
		float difficultyMultiplier = config.armorScalesWithDifficulty
				? getDifficultyMultiplier(difficulty) * waveBonus
				: waveBonus;

		double baseChance = Math.min(config.pillagerArmorChance * difficultyMultiplier, 0.9);
		int maxTier = getMaxTier(difficulty, config.armorScalesWithDifficulty);

		tryEquipSlot(mob, EquipmentSlot.HEAD, HELMETS, maxTier, baseChance, random);
		tryEquipSlot(mob, EquipmentSlot.CHEST, CHESTPLATES, maxTier, baseChance * 0.8, random);
		tryEquipSlot(mob, EquipmentSlot.LEGS, LEGGINGS, maxTier, baseChance * 0.6, random);
		tryEquipSlot(mob, EquipmentSlot.FEET, BOOTS, maxTier, baseChance * 0.7, random);
	}

	private static void tryEquipSlot(Mob mob, EquipmentSlot slot, Item[] tiers,
			int maxTier, double chance, RandomSource random) {
		if (mob.getItemBySlot(slot).isEmpty() && random.nextDouble() < chance) {
			int tier = random.nextInt(maxTier + 1);
			mob.setItemSlot(slot, new ItemStack(tiers[tier]));
			mob.setDropChance(slot, DROP_CHANCE);
		}
	}

	private static float getDifficultyMultiplier(Difficulty difficulty) {
		return switch (difficulty) {
			case PEACEFUL -> 0.0f;
			case EASY -> 0.5f;
			case NORMAL -> 1.0f;
			case HARD -> 1.5f;
		};
	}

	private static int getMaxTier(Difficulty difficulty, boolean scales) {
		if (!scales) return 3;
		return switch (difficulty) {
			case PEACEFUL -> 0;
			case EASY -> 1;
			case NORMAL -> 2;
			case HARD -> 3;
		};
	}
}
