package net.berkle.vanillaplusaccents.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared helpers for seat placement validation. */
public final class SeatSupport {

	private static final int REQUIRED_HEADROOM = 2;
	/** Sit just above the halfway surface (bottom slab/stair top at 0.5). */
	private static final double SEAT_Y_OFFSET = 0.02;
	/** Stairs: from block center toward the back face (opposite FACING). */
	private static final double STAIR_BACK_OFFSET = 0.2;
	/** Max rise from the player's feet to the seat — blocks climb-by-sitting. */
	private static final double MAX_SEAT_RISE = 2.0;

	private SeatSupport() {
	}

	public static boolean isSeatBlock(BlockState state) {
		if (state.getBlock() instanceof SlabBlock) {
			return true;
		}
		if (state.getBlock() instanceof StairBlock) {
			return state.getValue(StairBlock.HALF) == Half.BOTTOM;
		}
		return false;
	}

	public static boolean hasHeadroom(net.minecraft.world.level.Level level, BlockPos seatBlock) {
		for (int i = 1; i <= REQUIRED_HEADROOM; i++) {
			BlockPos above = seatBlock.above(i);
			if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** True if the seat is not more than {@link #MAX_SEAT_RISE} above the player's feet. */
	public static boolean isWithinSitReach(net.minecraft.world.entity.player.Player player, Vec3 seatPos) {
		double feetY = player.getY();
		if (feetY > seatPos.y + 0.25) {
			return false;
		}
		return seatPos.y <= feetY + MAX_SEAT_RISE;
	}

	/** Client/server pre-check before attempting to sit. */
	public static boolean canAttemptSit(
		net.minecraft.world.level.Level level,
		BlockPos pos,
		net.minecraft.world.entity.player.Player player
	) {
		BlockState state = level.getBlockState(pos);
		if (!isSeatBlock(state) || !hasHeadroom(level, pos)) {
			return false;
		}
		return isWithinSitReach(player, seatPosition(level, pos, state));
	}

	public static Vec3 seatPosition(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + seatSurfaceHeight(level, pos, state) + SEAT_Y_OFFSET;
		double z = pos.getZ() + 0.5;

		if (state.getBlock() instanceof StairBlock) {
			// Sit slightly forward of the tall back face (toward FACING from the rear).
			var facing = state.getValue(StairBlock.FACING);
			x -= facing.getStepX() * STAIR_BACK_OFFSET;
			z -= facing.getStepZ() * STAIR_BACK_OFFSET;
		}

		return new Vec3(x, y, z);
	}

	private static double seatSurfaceHeight(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof SlabBlock) {
			SlabType type = state.getValue(SlabBlock.TYPE);
			return switch (type) {
				case BOTTOM -> 0.5;
				case TOP -> 1.0;
				case DOUBLE -> 1.0;
			};
		}
		if (state.getBlock() instanceof StairBlock) {
			return 0.5;
		}
		VoxelShape shape = state.getCollisionShape(level, pos);
		return shape.isEmpty() ? 1.0 : shape.max(net.minecraft.core.Direction.Axis.Y);
	}

	public static boolean trySit(ServerPlayer player, BlockPos pos) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		if (!isSeatBlock(state) || !hasHeadroom(level, pos)) {
			return false;
		}

		Vec3 seatPos = seatPosition(level, pos, state);
		if (!isWithinSitReach(player, seatPos)) {
			return false;
		}

		player.stopRiding();
		SeatEntity seat = SeatEntity.spawn(level, pos, seatPos);
		return player.startRiding(seat, true, true);
	}
}
