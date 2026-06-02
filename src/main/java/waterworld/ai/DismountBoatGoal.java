package waterworld.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import java.util.EnumSet;

public class DismountBoatGoal extends Goal {
	private final Mob mob;

	public DismountBoatGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.noneOf(Flag.class));
	}

	@Override
	public boolean canUse() {
		return mob.getVehicle() instanceof AbstractBoat boat && shouldDismount(boat);
	}

	@Override
	public void start() {
		mob.stopRiding();
	}

	private boolean shouldDismount(AbstractBoat boat) {
		return boat.getStatus() == AbstractBoat.Status.ON_LAND;
	}
}
