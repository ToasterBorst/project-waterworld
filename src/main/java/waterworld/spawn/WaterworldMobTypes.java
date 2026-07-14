package waterworld.spawn;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

/**
 * Shared mob-type checks for boat AI and entity-load injection.
 */
public final class WaterworldMobTypes {
	private WaterworldMobTypes() {
	}

	public static boolean canDismountBoats(Mob mob) {
		return isHostileBoatPilot(mob)
				|| mob instanceof Ravager
				|| mob instanceof WanderingTrader
				|| mob instanceof TraderLlama;
	}

	public static boolean canPilotBoats(Mob mob) {
		return isHostileBoatPilot(mob) || mob instanceof WanderingTrader;
	}

	public static boolean isHostileBoatPilot(Mob mob) {
		return mob instanceof Pillager
				|| mob instanceof Vindicator
				|| mob instanceof Evoker
				|| mob instanceof Witch;
	}

	public static boolean shouldOpenDoors(Mob mob) {
		return isHostileBoatPilot(mob);
	}
}
