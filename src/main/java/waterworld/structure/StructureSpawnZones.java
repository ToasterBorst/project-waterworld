package waterworld.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Helpers for vanilla-sized spawn volumes centered on anchors inside NBT templates.
 */
public final class StructureSpawnZones {

	private StructureSpawnZones() {
	}

	/**
	 * Transforms a local-template AABB through placement rotation into world space.
	 */
	public static BoundingBox localBoxToWorld(BlockPos templatePos, Rotation rotation,
			int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		BoundingBox box = null;
		for (int x : new int[]{minX, maxX}) {
			for (int y : new int[]{minY, maxY}) {
				for (int z : new int[]{minZ, maxZ}) {
					BlockPos world = localToWorld(templatePos, rotation, new BlockPos(x, y, z));
					BoundingBox corner = new BoundingBox(world);
					box = box == null ? corner : BoundingBox.encapsulating(box, corner);
				}
			}
		}
		return box;
	}

	/**
	 * Axis-aligned box of {@code sizeX × sizeZ} with Y from {@code center.y - below}
	 * to {@code center.y + above} (inclusive).
	 */
	public static BoundingBox boxCentered(BlockPos center, int sizeX, int below, int above, int sizeZ) {
		int minX = center.getX() - (sizeX - 1) / 2;
		int maxX = minX + sizeX - 1;
		int minY = center.getY() - below;
		int maxY = center.getY() + above;
		int minZ = center.getZ() - (sizeZ - 1) / 2;
		int maxZ = minZ + sizeZ - 1;
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static BlockPos localToWorld(BlockPos templatePos, Rotation rotation, BlockPos local) {
		return templatePos.offset(rotate(local, rotation));
	}

	/** Matches {@code StructureTemplate} / {@code Rotation} block-offset transform. */
	private static BlockPos rotate(BlockPos pos, Rotation rotation) {
		return switch (rotation) {
			case NONE -> pos;
			case CLOCKWISE_90 -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
			case CLOCKWISE_180 -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
			case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
		};
	}
}
