package net.berkle.vanillaplusaccents.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Empty-hand Shift+right-click sitting on slabs and stairs. */
public final class SeatHandler {

	private SeatHandler() {
	}

	public static InteractionResult onUseBlock(
		Player player,
		Level level,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		if (hand != InteractionHand.MAIN_HAND
			|| !player.getMainHandItem().isEmpty()
			|| !player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();
		if (!SeatSupport.canAttemptSit(level, pos, player)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		return SeatSupport.trySit(serverPlayer, pos)
			? InteractionResult.SUCCESS_SERVER
			: InteractionResult.PASS;
	}
}
