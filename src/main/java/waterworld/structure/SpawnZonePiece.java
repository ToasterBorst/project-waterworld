package waterworld.structure;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Structure pieces that expose a vanilla-sized spawn volume separate from the
 * placement bounding box (template footprint). Placement BB must stay large
 * enough for multi-chunk {@code intersects} gating; spawn overrides should use
 * {@link #getSpawnZone()} instead.
 */
public interface SpawnZonePiece {
	BoundingBox getSpawnZone();
}
