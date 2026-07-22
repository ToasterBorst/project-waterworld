package waterworld.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.raid.Raider;
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

	public static boolean shouldEquipArmor(Mob mob) {
		return mob instanceof Raider raider && !(raider instanceof Ravager);
	}

	/**
	 * Equips armor on structure-spawned or naturally loaded illagers that missed
	 * patrol/raid hooks. Skips mobs that already wear armor.
	 */
	public static void tryEquipArmorOnLoad(Mob mob, ServerLevel level) {
		if (!shouldEquipArmor(mob)) return;
		if (hasAnyArmor(mob)) return;
		equipRandomArmor(mob, level.getDifficulty(), level.getRandom(), level.getGameTime());
	}

	public static void equipRandomArmor(Mob mob, Difficulty difficulty, RandomSource random, long gameTime) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.pillagerArmor) return;

		double dayScale = WaterworldConfig.dayScaleFactor(
				gameTime, config.patrolMinDays, config.patrolFullStrengthDays);
		float combinedMultiplier = getCombinedMultiplier(difficulty, dayScale, config.armorScalesWithDifficulty);
		int maxTier = getMaxTier(difficulty, dayScale, config.armorScalesWithDifficulty);

		double baseChance = Math.min(config.pillagerArmorChance * combinedMultiplier, 0.95);

		tryEquipSlot(mob, EquipmentSlot.HEAD, HELMETS, maxTier, baseChance, random);
		tryEquipSlot(mob, EquipmentSlot.CHEST, CHESTPLATES, maxTier, baseChance * 0.8, random);
		tryEquipSlot(mob, EquipmentSlot.LEGS, LEGGINGS, maxTier, baseChance * 0.6, random);
		tryEquipSlot(mob, EquipmentSlot.FEET, BOOTS, maxTier, baseChance * 0.7, random);
	}

	/**
	 * Higher chance variant for raid waves.
	 */
	public static void equipRandomArmorForRaid(Mob mob, Difficulty difficulty, RandomSource random,
			long gameTime, int wave) {
		WaterworldConfig config = WaterworldConfig.INSTANCE;
		if (!config.pillagerArmor) return;

		double dayScale = WaterworldConfig.dayScaleFactor(
				gameTime, config.patrolMinDays, config.patrolFullStrengthDays);
		float waveBonus = 1.0f + (wave * 0.1f);
		float combinedMultiplier = getCombinedMultiplier(difficulty, dayScale, config.armorScalesWithDifficulty)
				* waveBonus;

		double baseChance = Math.min(config.pillagerArmorChance * combinedMultiplier, 0.9);
		int maxTier = getMaxTier(difficulty, dayScale, config.armorScalesWithDifficulty);

		tryEquipSlot(mob, EquipmentSlot.HEAD, HELMETS, maxTier, baseChance, random);
		tryEquipSlot(mob, EquipmentSlot.CHEST, CHESTPLATES, maxTier, baseChance * 0.8, random);
		tryEquipSlot(mob, EquipmentSlot.LEGS, LEGGINGS, maxTier, baseChance * 0.6, random);
		tryEquipSlot(mob, EquipmentSlot.FEET, BOOTS, maxTier, baseChance * 0.7, random);
	}

	private static boolean hasAnyArmor(Mob mob) {
		return !mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
				|| !mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				|| !mob.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
				|| !mob.getItemBySlot(EquipmentSlot.FEET).isEmpty();
	}

	private static void tryEquipSlot(Mob mob, EquipmentSlot slot, Item[] tiers,
			int maxTier, double chance, RandomSource random) {
		if (mob.getItemBySlot(slot).isEmpty() && random.nextDouble() < chance) {
			int tier = random.nextInt(maxTier + 1);
			mob.setItemSlot(slot, new ItemStack(tiers[tier]));
			mob.setDropChance(slot, DROP_CHANCE);
		}
	}

	private static float getCombinedMultiplier(Difficulty difficulty, double dayScale, boolean scalesWithDifficulty) {
		float difficultyMultiplier = scalesWithDifficulty ? getDifficultyMultiplier(difficulty) : 1.0f;
		// 50% of configured chance at patrol start, full chance at patrol full-strength day.
		float dayMultiplier = (float) (0.5 + 0.5 * dayScale);
		return difficultyMultiplier * dayMultiplier;
	}

	private static float getDifficultyMultiplier(Difficulty difficulty) {
		return switch (difficulty) {
			case PEACEFUL -> 0.0f;
			case EASY -> 0.5f;
			case NORMAL -> 1.0f;
			case HARD -> 1.5f;
		};
	}

	private static int getMaxTier(Difficulty difficulty, double dayScale, boolean scales) {
		if (!scales) return 3;
		int baseTier = switch (difficulty) {
			case PEACEFUL -> 0;
			case EASY -> 1;
			case NORMAL -> 2;
			case HARD -> 3;
		};
		int dayBonus = (int) Math.floor(dayScale * 1.5);
		return Math.min(3, baseTier + dayBonus);
	}
}
